package fr.openent.form.core.models;

import fr.openent.form.core.enums.RgpdLifetimes;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Dates.YYYY_MM_DD_T_HH_MM_SS_SSS;
import static fr.openent.form.core.constants.Fields.*;

public class Form implements Model<Form> {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSS);

    private Number id;
    private String title;
    private String description;
    private String picture;
    private String ownerId;
    private String ownerName;
    private Date dateOpening;
    private Date dateEnding;
    private Boolean multiple;
    private Boolean anonymous;
    private Boolean reminded;
    private Boolean responseNotified;
    private Boolean editable;
    private Boolean rgpd;
    private String rgpdGoal;
    private RgpdLifetimes rgpdLifetime;
    private String publicKey;
    private Number originalFormId;

    private List<FormElement> formElements;


    // Constructors

    public Form() {}

    public Form(JsonObject form) {
        this.id = form.getNumber(ID, null);
        this.title = form.getString(TITLE,null);
        this.description = form.getString(DESCRIPTION, null);
        this.picture = form.getString(PICTURE, null);
        this.ownerId = form.getString(OWNER_ID, null);
        this.ownerName = form.getString(OWNER_NAME, null);
        try {
            this.dateOpening = dateFormatter.parse(form.getString(DATE_OPENING, null));
            this.dateEnding = dateFormatter.parse(form.getString(DATE_ENDING, null));
        }
        catch (ParseException e) { e.printStackTrace(); }
        this.multiple = form.getBoolean(MULTIPLE, null);
        this.anonymous = form.getBoolean(ANONYMOUS, null);
        this.reminded = form.getBoolean(REMINDED, null);
        this.responseNotified = form.getBoolean(RESPONSE_NOTIFIED, null);
        this.rgpd = form.getBoolean(RGPD, null);
        this.rgpdGoal = form.getString(RGPD_GOAL, null);
        this.rgpdLifetime = RgpdLifetimes.getRgpdLifetimes(form.getInteger(RGPD_LIFETIME, null));
        this.publicKey = form.getString(PUBLIC_KEY, null);
        this.originalFormId = form.getNumber(ORIGINAL_FORM_ID,null);
        this.formElements = this.toListFormElements(form.getJsonArray(FORM_ELEMENTS, null));
    }


    // Getters

    public SimpleDateFormat getDateFormatter() { return dateFormatter; }

    public Number getId() { return id; }

    public String getTitle() { return title;}

    public String getDescription() { return description; }

    public String getPicture() { return picture; }

    public String getOwnerId() { return ownerId; }

    public String getOwnerName() { return ownerName; }

    public Date getDateOpening() { return dateOpening; }

    public Date getDateEnding() { return dateEnding; }

    public Boolean getMultiple() { return multiple; }

    public Boolean getAnonymous() { return anonymous; }

    public Boolean getReminded() { return reminded; }

    public Boolean getResponseNotified() { return responseNotified; }

    public Boolean getEditable() { return editable; }

    public Boolean getRgpd() { return rgpd; }

    public String getRgpdGoal() { return rgpdGoal; }

    public Number getRgpdLifetime() { return rgpdLifetime.getValue(); }

    public String getPublicKey() { return publicKey; }

    public Number getOriginalFormId() { return originalFormId; }

    public List<FormElement> getFormElements() { return formElements; }


    // Setters

    public Form setId(Number id) {
        this.id = id;
        return this;
    }

    public Form setTitle(String title) {
        this.title = title;
        return this;
    }

    public Form setDescription(String description) {
        this.description = description;
        return this;
    }

    public Form setPicture(String picture) {
        this.picture = picture;
        return this;
    }

    public Form setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public Form setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
    }

    public Form setDateOpening(Date dateOpening) {
        this.dateOpening = dateOpening;
        return this;
    }

    public Form setDateEnding(Date dateEnding) {
        this.dateEnding = dateEnding;
        return this;
    }

    public Form setMultiple(Boolean multiple) {
        this.multiple = multiple;
        return this;
    }

    public Form setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
        return this;
    }

    public Form setReminded(Boolean reminded) {
        this.reminded = reminded;
        return this;
    }

    public Form setResponseNotified(Boolean responseNotified) {
        this.responseNotified = responseNotified;
        return this;
    }

    public Form setEditable(Boolean editable) {
        this.editable = editable;
        return this;
    }

    public Form setRgpd(Boolean rgpd) {
        this.rgpd = rgpd;
        return this;
    }

    public Form setRgpdGoal(String rgpdGoal) {
        this.rgpdGoal = rgpdGoal;
        return this;
    }

    public Form setRgpdLifetime(RgpdLifetimes rgpdLifetime) {
        this.rgpdLifetime = rgpdLifetime;
        return this;
    }

    public Form setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public Form setOriginalFormId(Number originalFormId) {
        this.originalFormId = originalFormId;
        return this;
    }

    public Form setFormElements(List<FormElement> formElements) {
        this.formElements = formElements;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(TITLE, this.title)
                .put(DESCRIPTION, this.description)
                .put(PICTURE, this.picture)
                .put(OWNER_ID, this.ownerId)
                .put(OWNER_NAME, this.ownerName)
                .put(DATE_OPENING, this.dateOpening)
                .put(DATE_ENDING, this.dateEnding)
                .put(MULTIPLE, this.multiple)
                .put(ANONYMOUS, this.anonymous)
                .put(REMINDED, this.reminded)
                .put(RGPD, this.rgpd)
                .put(RGPD_GOAL, this.rgpdGoal)
                .put(RGPD_LIFETIME, this.rgpdLifetime.getValue())
                .put(PUBLIC_KEY, this.publicKey)
                .put(ORIGINAL_FORM_ID, this.originalFormId)
                .put(FORM_ELEMENTS, this.toJsonArrayFormElements(this.formElements));
    }

    @Override
    public Form model(JsonObject form){
        return new Form(form);
    }

    private List<FormElement> toListFormElements(JsonArray formElements) {
        return formElements.stream()
                .map(formElement -> {
                    if (formElement instanceof JsonObject) {
                        JsonObject jsonFormElement = (JsonObject)formElement;
                        return jsonFormElement.containsKey(DESCRIPTION) ? new Section(jsonFormElement) : new Question(jsonFormElement);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private JsonArray toJsonArrayFormElements(List<FormElement> formElements) {
        return new JsonArray(formElements.stream().map(FormElement::toJson).collect(Collectors.toList()));
    }
}

