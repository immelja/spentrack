package com.immelja;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Transaction {
	private String dateString;
	private Date date;
	private int finYear;
	private int reportingPeriod;
	private float amount;
	private String description;
	private float balance;
	private String category;
	private String income;
	private String term;
	   private String term2;

	private String key;
	public Transaction(){}
	public Transaction(String dateString, float amount, String description, float balance, String category, String term, String term2) {
		super();
		this.dateString = dateString;
		this.amount = amount;
		this.description = description;
		this.balance = balance;
		this.category = category;
		
		this.income = amount>0?"INCOME":"EXPENSE";
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		try {
			date = format.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Format formatter = new SimpleDateFormat("yyyy"); 
		this.finYear = Integer.valueOf(formatter.format(date));
		this.reportingPeriod = date.getMonth();
		this.term = term;
	      this.term = term2;

	}
	public void setAmount(float amount) {
        this.amount = amount;
    }
    public Date getDate() {
		return date;
	}
	public int getFinYear() {
	    Format mmFormat = new SimpleDateFormat("MM"); 
	    int mm = Integer.valueOf(mmFormat.format(date));
	    Format yyyyFormat = new SimpleDateFormat("yyyy"); 
	    finYear = Integer.valueOf(yyyyFormat.format(date))+((mm>6)?1:0);
        return finYear;
    }
    public void setFinYear(int finYear) {
        this.finYear = finYear;
    }
    public int getReportingPeriod() {
        Format formatter = new SimpleDateFormat("yyyyMM"); 
        reportingPeriod = Integer.valueOf(formatter.format(date));
        return reportingPeriod;
    }
    public void setReportingPeriod(int reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }
    public float getAmount() {
		return amount;
	}
	public String getDescription() {
		return description;
	}
	public float getBalance() {
		return balance;
	}
	public String getCategory() {
		return category;
	}
	
	public String getIncome() {
		return income;
	}	
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	
	public String getTerm2() {
        return term2;
    }
    public void setTerm2(String term2) {
        this.term2 = term2;
    }
    public String getKey() {
		return date.getTime() + "_" + amount + "_" + description.replace(" ", "");
	}
	@Override
	public String toString() {
		return date.toString() + " " + amount + " " + description + " " + category + " " + income;
	}
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(amount);
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transaction other = (Transaction) obj;
        if (Float.floatToIntBits(amount) != Float.floatToIntBits(other.amount))
            return false;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        return true;
    }
	
	
	
	

}
