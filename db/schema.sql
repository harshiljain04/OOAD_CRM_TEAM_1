CREATE TABLE leads (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) UNIQUE,
    status      ENUM('NEW','QUALIFIED','WON','LOST') DEFAULT 'NEW',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    lead_id     INT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) UNIQUE,
    ltv         DECIMAL(10,2) DEFAULT 0.00,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id) REFERENCES leads(id)
);

CREATE TABLE campaigns (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    budget      DECIMAL(10,2),
    revenue     DECIMAL(10,2) DEFAULT 0.00,
    start_date  DATE,
    end_date    DATE
);

-- YOUR primary table
CREATE TABLE interactions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    lead_id     INT,
    customer_id INT,
    type        VARCHAR(50) NOT NULL,  -- 'call', 'email', 'meeting'
    notes       TEXT,
    timestamp   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id)     REFERENCES leads(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- create all tables first
CREATE TABLE leads (...);
CREATE TABLE customers (...);
CREATE TABLE campaigns (...);
CREATE TABLE interactions (...);

-- THEN modify existing table
ALTER TABLE interactions DROP FOREIGN KEY interactions_ibfk_2;
ALTER TABLE interactions MODIFY customer_id INT DEFAULT NULL;