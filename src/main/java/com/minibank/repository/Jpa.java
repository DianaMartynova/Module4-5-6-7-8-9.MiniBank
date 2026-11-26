package com.minibank.repository;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class Jpa {

    private static final EntityManagerFactory FACTORY =
            Persistence.createEntityManagerFactory("minibank");

    private Jpa() {
    }

    public static EntityManagerFactory entityManagerFactory() {
        return FACTORY;
    }
}
