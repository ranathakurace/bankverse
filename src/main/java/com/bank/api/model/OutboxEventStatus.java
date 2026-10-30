package com.bank.api.model;

/** Lifecycle for a durable domain event waiting to cross a service boundary. */
public enum OutboxEventStatus { PENDING, PUBLISHED, FAILED }
