package com.sysalto.report.examples;

import java.sql.*;

public class MutualFundsInitData {
    static Connection conn = null;

    static private void dbUpdate(String sql) throws SQLException {
        Statement st = conn.createStatement();
        st.executeUpdate(sql);
        st.close();
    }

    static public ResultSet query(String sql) throws SQLException {
        Statement st = conn.createStatement();
        return st.executeQuery(sql);
    }

    static private void init() throws Exception {
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        conn = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");
    }

    static private void initDb1() throws SQLException {
        dbUpdate("create table clnt ( " +
                "name varchar(255)," +
                "addr1 varchar(255)," +
                "addr2 varchar(255)," +
                "addr3 varchar(255)," +
                "accountNbr integer," +
                "branch_addr1 varchar(255)," +
                "branch_addr2 varchar(255)," +
                "branch_addr3 varchar(255)," +
                "benef_name varchar(255))");

        dbUpdate("insert into  clnt ( " +
                "name, addr1, addr2, addr3, accountNbr, branch_addr1, branch_addr2, branch_addr3, benef_name ) " +
                "values('John Mill', ' 1 Main', 'New York,NY', '200100', 123, '10 Main'," +
                "'New York,NY', '22111', 'Mary Mill')");
        //sum investment table
        dbUpdate("create table sum_investment ( " +
                "fund_name varchar(255)," +
                "value1 numeric,value2 numeric)");

        dbUpdate("insert into  sum_investment ( " +
                "fund_name, value1, value2 ) " +
                "values( 'Money Market Fund', 1000.0, 1200.0)");
        dbUpdate("insert into  sum_investment ( " +
                "fund_name, value1, value2 ) " +
                "values('Fixed Income Funds', 18544.44, 18826.21)");
        dbUpdate("insert into  sum_investment ( " +
                "fund_name, value1, value2 ) " +
                "values( 'Balanced Funds', 12345.65, 12423.0)");
        dbUpdate("insert into  sum_investment ( " +
                "fund_name, value1, value2) " +
                "values( 'Equity Funds', 2340, 2500)");
        dbUpdate("insert into  sum_investment (" +
                "fund_name, value1, value2 ) " +
                "values('US Funds', 2000, 2050)");
        dbUpdate("insert into  sum_investment ( " +
                "fund_name, value1, value2 )" +
                "values('International Equity Funds', 3200, 3250)");
        dbUpdate("insert into  sum_investment ( " +
                "fund_name, value1, value2 )" +
                "values('Global Equity Funds', 4000, 4025)");
        dbUpdate("create table tran_account ( " +
                "name varchar(255),value1 numeric,value2 numeric,value3 numeric) ");
        dbUpdate("insert into  tran_account ( " +
                "name, value1, value2, value3 ) " +
                "values('Begining account value', 12300.0, 13300.0, 13300.0)");
        dbUpdate("insert into  tran_account ( " +
                "name, value1, value2, value3 ) " +
                "values('Amount in', 780, 900, 900)");
        dbUpdate("insert into  tran_account ( " +
                "name, value1, value2, value3 ) " +
                "values('Amount out', 0, 0, 0)");
        dbUpdate("insert into  tran_account ( " +
                "name, value1, value2, value3 )" +
                "values('Change in the value of your account', 500, 700, 700)");
        dbUpdate("create table account_perf ( " +
                "value3m varchar(255)," +
                "value1y varchar(255),value3y varchar (255),value5y varchar (255)," +
                "value10y varchar (255),annualized varchar (255))");
        dbUpdate("insert into  account_perf ( " +
                "value3m, value1y, value3y, value5y, value10y, annualized) " +
                "values('2.22', 'N/A', 'N/A', 'N/A', 'N/A', '5')");
    }

    static public void initDb() {
        try {
            init();
            initDb1();
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}