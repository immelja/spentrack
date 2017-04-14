package com.immelja;

import java.util.Date;
import java.util.List;
import java.util.TreeSet;

public class Account {
    private String type;
    private float balance;
    private List<Transaction> transactions;

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List<Transaction> getTransactions() {
		return transactions;
	}
	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}
    public float getBalance() {
        return balance;
    }
    public void setBalance(float balance) {
        this.balance = balance;
    }
    public Date getDate(String firstLast) {
        TreeSet dateSet = new TreeSet<Date>();
        for(Transaction transaction:getTransactions()) {
            dateSet.add(transaction.getDate());
        }
        if (firstLast.equals("FIRST")) {
            return (Date) dateSet.first();
        } else {
            return (Date) dateSet.last();
        }
    }
    

    
}
