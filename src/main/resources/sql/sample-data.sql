-- Sample data for budget_transactions (template, generic merchants and details)

INSERT INTO budget_transactions
(name, amount, category, criticality, criticality_id, transaction_date, account, status, created_time, payment_method, statement_period, row_hash)
VALUES
('ElectroMart Purchase', 59.99, 'Electronics', 'Nonessential', 2, '2025-10-01', 'MainCard', 'Completed', '2025-10-02 10:00:00', 'CreditCard', '2025-10', 'a1b2c3d4e5f60123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Bookstore Order', 28.50, 'Books', 'Nonessential', 2, '2025-10-02', 'StudentSavings', 'Completed', '2025-10-02 11:15:00', 'DebitCard', '2025-10', 'b2c3d4e5f6a70123456789abcdef0123456789abcdef0123456789abcdef0123'),
('SuperMart Groceries', 102.43, 'Groceries', 'Essential', 1, '2025-10-03', 'Joint', 'Completed', '2025-10-03 17:30:00', 'Visa', '2025-10', 'c3d4e5f6a7b80123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Fuel Station', 45.00, 'Gas', 'Essential', 1, '2025-10-04', 'MainCard', 'Completed', '2025-10-04 08:00:00', 'MasterCard', '2025-10', 'd4e5f6a7b8c90123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Coffee Shop', 6.50, 'Dining out', 'Nonessential', 2, '2025-10-05', 'RewardsCard', 'Completed', '2025-10-05 09:45:00', 'Amex', '2025-10', 'e5f6a7b8c9d00123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Gym Membership', 29.99, 'Fitness', 'Essential', 1, '2025-10-06', 'Health', 'Active', '2025-10-06 07:00:00', 'DirectDebit', '2025-10', 'f6a7b8c9d0e10123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Movie Night', 15.00, 'Entertainment', 'Nonessential', 2, '2025-10-07', 'MainCard', 'Completed', '2025-10-07 20:00:00', 'CreditCard', '2025-10', 'a7b8c9d0e1f20123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Subscription Service', 12.99, 'Subscription', 'Nonessential', 2, '2025-10-08', 'Joint', 'Active', '2025-10-08 12:00:00', 'PayPal', '2025-10', 'b8c9d0e1f2a30123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Pharmacy', 22.75, 'Health', 'Essential', 1, '2025-10-09', 'RewardsCard', 'Completed', '2025-10-09 16:10:00', 'Visa', '2025-10', 'c9d0e1f2a3b40123456789abcdef0123456789abcdef0123456789abcdef0123'),
('Home Supplies', 38.20, 'Household', 'Essential', 1, '2025-10-10', 'MainCard', 'Completed', '2025-10-10 14:00:00', 'MasterCard', '2025-10', 'd0e1f2a3b4c50123456789abcdef0123456789abcdef0123456789abcdef0123');
