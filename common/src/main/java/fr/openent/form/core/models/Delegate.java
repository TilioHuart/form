package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class Delegate implements IModel<Delegate> {
    private Number id;
    private String entity;
    private String mail;
    private String address;
    private Number zipcode;
    private String city;


    // Constructors

    public Delegate() {}

    public Delegate(JsonObject delegate) {
        this.id = delegate.getNumber(ID, null);
        this.entity = delegate.getString(ENTITY, null);
        this.mail = delegate.getString(MAIL, null);
        this.address = delegate.getString(ADDRESS, null);
        this.zipcode = delegate.getNumber(ZIPCODE, null);
        this.city = delegate.getString(CITY, null);
    }


    // Getters

    public Number getId() { return id; }

    public String getEntity() { return entity; }

    public String getMail() { return mail; }

    public String getAddress() { return address; }

    public Number getZipcode() { return zipcode; }

    public String getCity() { return city; }


    // Setters

    public Delegate setId(Number id) {
        this.id = id;
        return this;
    }

    public Delegate setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public Delegate setMail(String mail) {
        this.mail = mail;
        return this;
    }

    public Delegate setAddress(String address) {
        this.address = address;
        return this;
    }

    public Delegate setZipcode(Number zipcode) {
        this.zipcode = zipcode;
        return this;
    }

    public Delegate setAnswer(String city) {
        this.city = city;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(ENTITY, this.entity)
                .put(MAIL, this.mail)
                .put(ADDRESS, this.address)
                .put(ZIPCODE, this.zipcode)
                .put(CITY, this.city);
    }

    @Override
    public Delegate model(JsonObject delegate){
        return new Delegate(delegate);
    }
}

