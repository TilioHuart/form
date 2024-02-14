package fr.openent.form.core.models;

import fr.openent.form.core.enums.RgpdLifetimes;
import fr.openent.form.helpers.DateHelper;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Date;
import java.util.List;
import static fr.openent.form.core.constants.DateFormats.*;
import static fr.openent.form.core.constants.Fields.*;

public class Form implements IModel<Form> {
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
    private Boolean archived;
    private Boolean sent;
    private Boolean collab;
    private String rgpdGoal;
    private RgpdLifetimes rgpdLifetime;
    private Boolean isPublic;
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
        this.dateOpening = DateHelper.formatDateToModel(form.getString(DATE_OPENING, null), YYYY_MM_DD_T_HH_MM_SS_SSS);
        this.dateEnding =  DateHelper.formatDateToModel(form.getString(DATE_ENDING, null), YYYY_MM_DD_T_HH_MM_SS_SSS);
        this.multiple = form.getBoolean(MULTIPLE, false);
        this.anonymous = form.getBoolean(ANONYMOUS, false);
        this.reminded = form.getBoolean(REMINDED, false);
        this.responseNotified = form.getBoolean(RESPONSE_NOTIFIED, false);
        this.rgpd = form.getBoolean(RGPD, false);
        this.archived = form.getBoolean(ARCHIVED, false);
        this.sent = form.getBoolean(SENT, false);
        this.collab = form.getBoolean(COLLAB, false);
        this.editable = form.getBoolean(EDITABLE, false);
        this.rgpdGoal = form.getString(RGPD_GOAL, null);
        this.rgpdLifetime = RgpdLifetimes.getRgpdLifetimes(form.getInteger(RGPD_LIFETIME, null));
        this.isPublic = form.getBoolean(IS_PUBLIC, null);
        this.publicKey = form.getString(PUBLIC_KEY, null);
        this.originalFormId = form.getNumber(ORIGINAL_FORM_ID,null);

        if (form.getValue(FORM_ELEMENTS, null) instanceof JsonArray) {
            this.formElements = FormElement.toListFormElements(form.getJsonArray(FORM_ELEMENTS, null));
        }
        else if (form.getValue(FORM_ELEMENTS, null) instanceof JsonObject && form.getJsonObject(FORM_ELEMENTS, null).containsKey(ARR)) {
            this.formElements = FormElement.toListFormElements(form.getJsonObject(FORM_ELEMENTS, null).getJsonArray(ARR));
        }
        else if (form.getValue(FORM_ELEMENTS, null) instanceof JsonObject && form.getJsonObject(FORM_ELEMENTS, null).containsKey(ALL)) {
            this.formElements = FormElement.toListFormElements(form.getJsonObject(FORM_ELEMENTS, null).getJsonArray(ALL));
        }
    }


    // Getters

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

    public Boolean getArchived() { return archived; }

    public Boolean getSent() { return sent; }

    public Boolean getCollab() { return collab; }

    public String getRgpdGoal() { return rgpdGoal; }

    public Number getRgpdLifetime() { return rgpdLifetime.getValue(); }

    public Boolean getIsPublic() { return this.isPublic; }

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

    public Form setArchived(Boolean archived) {
        this.archived = archived;
        return this;
    }
    public Form setSent(Boolean sent) {
        this.sent = sent;
        return this;
    }

    public Form setCollab(Boolean collab) {
        this.collab = collab;
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

    public Form setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
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
        boolean snakeCase = true;
        JsonObject result = IModelHelper.toJson(this, false, snakeCase);
        result.put(snakeCase ? DATE_OPENING : PARAM_DATE_OPENING, this.dateOpening != null ? this.dateOpening.toString() : null)
                .put(snakeCase ? DATE_ENDING : PARAM_DATE_ENDING, this.dateEnding != null ? this.dateEnding.toString() : null);
        return result;
    }

    @Override
    public Form model(JsonObject form){
        return new Form(form);
    }
}

