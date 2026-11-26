package com.minibank.repository;

import com.minibank.exception.BankDataException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NotFoundException;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.model.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BankService {

    private final EntityManagerFactory factory;

    public BankService(EntityManagerFactory factory) {
        this.factory = factory;
    }

    public Client registerClient(String name, String email) {
        return inTransaction("Не удалось зарегистрировать клиента " + email, em -> {
            Client client = new Client(name, email);
            em.persist(client);
            return client;
        });
    }

    public Account openAccount(Long clientId, String accountNumber) {
        return inTransaction("Не удалось открыть счёт " + accountNumber, em -> {
            Account account = new Account(require(em, Client.class, clientId, "Клиент не найден: "), accountNumber);
            em.persist(account);
            return account;
        });
    }

    public Account getAccount(Long accountId) {
        return read("Не удалось загрузить счёт " + accountId,
                em -> require(em, Account.class, accountId, "Счёт не найден: "));
    }

    public Client getClient(Long clientId) {
        return read("Не удалось загрузить клиента " + clientId,
                em -> require(em, Client.class, clientId, "Клиент не найден: "));
    }

    /**
     * Клиенты с номерами счетов. joinFetch = false показывает N+1, true решает проблему.
     * Коллекции читаются внутри открытого EntityManager, иначе будет LazyInitializationException.
     */
    public List<Map<String, Object>> listClients(boolean joinFetch) {
        String jpql = joinFetch
                ? "select c from Client c left join fetch c.accounts order by c.id"
                : "select c from Client c order by c.id";
        return read("Не удалось загрузить клиентов", em -> em.createQuery(jpql, Client.class)
                .getResultStream()
                .map(client -> Map.<String, Object>of(
                        "id", client.getId(),
                        "email", client.getEmail(),
                        "accounts", client.getAccounts().stream().map(Account::getAccountNumber).toList()))
                .toList());
    }

    public void removeAccount(Long accountId) {
        inTransaction("Не удалось удалить счёт " + accountId, em -> {
            Account account = require(em, Account.class, accountId, "Счёт не найден: ");
            account.getClient().removeAccount(account);
            return null;
        });
    }

    public void deposit(Long accountId, BigDecimal amount) {
        requirePositive(amount);
        inTransaction("Не удалось пополнить счёт " + accountId, em -> {
            Account account = lockAccount(em, accountId);
            apply(em, account, amount, "DEPOSIT");
            return null;
        });
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        requirePositive(amount);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Нельзя переводить деньги на тот же счёт: " + fromAccountId);
        }
        inTransaction("Не удалось выполнить перевод: " + fromAccountId + " -> " + toAccountId, em -> {
            // Блокируем в порядке возрастания id, чтобы не получить deadlock
            Account first = lockAccount(em, Math.min(fromAccountId, toAccountId));
            Account second = lockAccount(em, Math.max(fromAccountId, toAccountId));
            Account from = first.getId().equals(fromAccountId) ? first : second;
            Account to = from == first ? second : first;

            if (from.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Недостаточно средств на счёте " + fromAccountId);
            }
            apply(em, from, amount.negate(), "TRANSFER_OUT");
            apply(em, to, amount, "TRANSFER_IN");
            return null;
        });
    }

    public List<Transaction> getTransactions(Long accountId, LocalDateTime from, LocalDateTime to) {
        return read("Не удалось загрузить транзакции счёта " + accountId, em -> em.createQuery(
                        "select t from Transaction t "
                                + "where t.account.id = :accountId and t.timestamp >= :from and t.timestamp < :to "
                                + "order by t.timestamp, t.id", Transaction.class)
                .setParameter("accountId", accountId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList());
    }

    private Account lockAccount(EntityManager em, Long accountId) {
        return em.createQuery("select a from Account a where a.id = :id", Account.class)
                .setParameter("id", accountId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Счёт не найден: " + accountId));
    }

    /** Баланс обновится при commit благодаря dirty checking. */
    private void apply(EntityManager em, Account account, BigDecimal delta, String type) {
        account.setBalance(account.getBalance().add(delta));
        em.persist(new Transaction(account, delta, type, LocalDateTime.now()));
    }

    private static <T> T require(EntityManager em, Class<T> type, Long id, String message) {
        T entity = em.find(type, id);
        if (entity == null) {
            throw new NotFoundException(message + id);
        }
        return entity;
    }

    private <T> T read(String errorMessage, Function<EntityManager, T> work) {
        try (EntityManager em = factory.createEntityManager()) {
            return work.apply(em);
        } catch (PersistenceException e) {
            throw new BankDataException(errorMessage, e);
        }
    }

    private <T> T inTransaction(String errorMessage, Function<EntityManager, T> work) {
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            try {
                T result = work.apply(em);
                tx.commit();
                return result;
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
            }
        } catch (PersistenceException e) {
            throw new BankDataException(errorMessage, e);
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной: " + amount);
        }
    }
}
