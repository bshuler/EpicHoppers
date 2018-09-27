package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDatabase {

    private final EpicHoppersPlugin instance;

    private Connection connection;

    public MySQLDatabase(EpicHoppersPlugin instance) {
        this.instance = instance;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://" + instance.getConfig().getString("Database.IP") + ":" + instance.getConfig().getString("Database.Port") + "/" + instance.getConfig().getString("Database.Database Name") + "?autoReconnect=true&useSSL=false";
            this.connection = DriverManager.getConnection(url, instance.getConfig().getString("Database.Username"), instance.getConfig().getString("Database.Password"));

            //ToDo: This is sloppy
            connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `sync` (\n" +
                    "\t`location` TEXT NULL,\n" +
                    "\t`level` INT NULL,\n" +
                    "\t`block` TEXT NULL,\n" +
                    "\t`placedby` TEXT NULL,\n" +
                    "\t`player` TEXT NULL,\n" +
                    "\t`teleporttrigger` TEXT NULL,\n" +
                    "\t`autocrafting` TEXT NULL,\n" +
                    "\t`whitelist` TEXT NULL,\n" +
                    "\t`blacklist` TEXT NULL,\n" +
                    "\t`void` TEXT NULL,\n" +
                    "\t`black` TEXT NULL\n" +
                    ")");

            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `boosts` (\n" +
                    "\t`endtime` TEXT NULL,\n" +
                    "\t`amount` INT NULL,\n" +
                    "\t`uuid` TEXT NULL\n" +
                    ")");

        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Database connection failed.");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}