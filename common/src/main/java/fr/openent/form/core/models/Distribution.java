package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.openent.form.core.constants.DateFormats.YYYY_MM_DD_T_HH_MM_SS_SSS;

public class Distribution implements IModel<Distribution> {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSS);

    private Long id;
    private Long formId;
    private String senderId;
    private String senderName;
    private String responderId;
    private String responderName;
    private String status;
    private Date dateSending;
    private Date dateResponse;
    private Boolean active;
    private String structure;
    private Long originalId;
    private String publicKey;
    private Long captchaId;


    // Constructors

    public Distribution() {}

    public Distribution(JsonObject distribution) {
        this.id = distribution.getLong(ID, null);
        this.formId = distribution.getLong(FORM_ID,null);
        this.senderId = distribution.getString(SENDER_ID,null);
        this.senderName = distribution.getString(SENDER_NAME, null);
        this.responderId = distribution.getString(RESPONDER_ID, null);
        this.responderName = distribution.getString(RESPONDER_NAME, null);
        this.status = distribution.getString(STATUS, null);
        try {
            this.dateSending = distribution.getString(DATE_SENDING, null) != null ? dateFormatter.parse(distribution.getString(DATE_SENDING, null)) : null;
            this.dateResponse = distribution.getString(DATE_RESPONSE, null) != null ? dateFormatter.parse(distribution.getString(DATE_RESPONSE, null)) : null;
        }
        catch (ParseException e) { e.printStackTrace(); }
        this.active = distribution.getBoolean(ACTIVE, null);
        this.structure = distribution.getString(STRUCTURE, null);
        this.originalId = distribution.getLong(ORIGINAL_ID,null);
        this.publicKey = distribution.getString(PUBLIC_KEY, null);
        this.captchaId = distribution.getLong(CAPTCHA_ID,null);
    }


    // Getters

    public Long getId() {return id; }

    public Long getFormId() { return formId; }

    public String getSenderId() { return senderId; }

    public String getSenderName() { return senderName; }

    public String getResponderId() { return responderId; }

    public String getResponderName() { return responderName; }

    public String getStatus() { return status; }

    public Date getDateSending() { return dateSending; }

    public Date getDateResponse() { return dateResponse; }

    public Boolean getActive() { return active; }

    public String getStructure() { return structure; }

    public Long getOriginalId() { return originalId; }

    public String getPublicKey() { return publicKey; }

    public Long getCaptchaId() { return captchaId; }


    // Setters


    public Distribution setId(Long id) {
        this.id = id;
        return this;
    }

    public Distribution setFormId(Long formId) {
        this.formId = formId;
        return this;
    }

    public Distribution setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public Distribution setSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public Distribution setResponderId(String responderId) {
        this.responderId = responderId;
        return this;
    }

    public Distribution setResponderName(String responderName) {
        this.responderName = responderName;
        return this;
    }

    public Distribution setStatus(String status) {
        this.status = status;
        return this;
    }

    public Distribution setDateSending(Date dateSending) {
        this.dateSending = dateSending;
        return this;
    }

    public Distribution setDateResponse(Date dateResponse) {
        this.dateResponse = dateResponse;
        return this;
    }

    public Distribution setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public Distribution setStructure(String structure) {
        this.structure = structure;
        return this;
    }

    public Distribution setOriginalId(Long originalId) {
        this.originalId = originalId;
        return this;
    }

    public Distribution setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public Distribution setCaptchaId(Long captchaId) {
        this.captchaId = captchaId;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(FORM_ID, this.formId)
                .put(SENDER_ID, this.senderId)
                .put(SENDER_NAME, this.senderName)
                .put(RESPONDER_ID, this.responderId)
                .put(RESPONDER_NAME, this.responderName)
                .put(STATUS, this.status)
                .put(DATE_SENDING, this.dateSending != null ? this.dateSending.toString() : null)
                .put(DATE_RESPONSE, this.dateResponse != null ? this.dateResponse.toString() : null)
                .put(ACTIVE, this.active)
                .put(STRUCTURE, this.structure)
                .put(ORIGINAL_ID, this.originalId)
                .put(PUBLIC_KEY, this.publicKey)
                .put(CAPTCHA_ID, this.captchaId);
    }

    @Override
    public Distribution model(JsonObject distribution){
        return new Distribution(distribution);
    }
}

