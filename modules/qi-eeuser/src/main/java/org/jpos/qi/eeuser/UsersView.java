/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2017 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.qi.eeuser;

import com.vaadin.data.validator.EmailValidator;
import com.vaadin.ui.*;

import com.vaadin.ui.Grid;
import com.vaadin.v7.data.fieldgroup.BeanFieldGroup;
import com.vaadin.v7.data.fieldgroup.FieldGroup;
import com.vaadin.v7.data.fieldgroup.FieldGroupFieldFactory;
import com.vaadin.v7.data.util.ObjectProperty;
import com.vaadin.v7.data.util.PropertysetItem;
import com.vaadin.server.FontAwesome;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.OptionGroup;

import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.ui.PasswordField;
import com.vaadin.v7.ui.VerticalLayout;
import org.jpos.ee.*;
import org.jpos.qi.*;
import org.jpos.qi.components.QIFieldFactory;
import org.jpos.util.PasswordGenerator;

import java.util.Set;

public class UsersView extends QIEntityView<User> {

    private User selectedU;
    private FieldGroup passwordFieldGroup;
    private Panel passwordPanel;
    private Button changePassBtn;
    private Button resetPassBtn;
    private boolean forcePasswordChange;

    public UsersView () {
        super(User.class, "users");
    }

    @Override
    public String getHeaderSpecificTitle(Object entity) {
        if (entity instanceof User) {
            User u = (User) entity;
            return u.getNick() != null ? u.getId() + " - " + u.getNick() : "New";
        } else {
            return null;
        }
    }

    @Override
    public Object getEntity(Object entity) {
        if(entity instanceof User) {
            User u = (User) entity;
            if(u.getId() != null) {
                return getHelper().getEntityByParam(String.valueOf(u.getId()));
            }
        }
        return null;
    }

    @Override
    public User createNewEntity() {
        return new User();
    }

    @Override
    public QIHelper createHelper() {
        return new UsersHelper();
    }

    @Override
    public void saveEntity(Object entity) throws BLException {
        QI app = getApp();
        String generatedPassword = PasswordGenerator.generateRandomPassword();
        ((UsersHelper) getHelper()).saveUser(getBinder().getBean(), generatedPassword);
        showGeneratedPassword(generatedPassword);
        app.displayNotification(app.getMessage("created", getEntityName()));
        app.getNavigator().navigateTo(getGeneralRoute());
    }

    @Override
    public void updateEntity(BeanFieldGroup fieldGroup) throws
            FieldGroup.CommitException, CloneNotSupportedException, BLException
    {
        String current = "";
        String repeat = "";
        if (passwordFieldGroup != null) {
            Field currentPass = passwordFieldGroup.getField("current");
            Field repeatPass = passwordFieldGroup.getField("repeat");
            current = currentPass != null ? (String) currentPass.getValue() : "";
            repeat = repeatPass != null ? (String) repeatPass.getValue() : "";
        }
        if (((UsersHelper)getHelper()).updateUser(fieldGroup, current, repeat)){
            getApp().displayNotification(getApp().getMessage("updated", getEntityName().toUpperCase()));
            if (getApp().getUser().equals(fieldGroup.getItemDataSource().getBean())) {
                getApp().getUser().setName(((User) fieldGroup.getItemDataSource().getBean()).getName());
                getApp().getHeader().refresh();
            }
        }
        else
            getApp().displayNotification(getApp().getMessage("notchanged"));
    }


    @Override
    public FieldGroupFieldFactory createFieldFactory() {
        return new QIFieldFactory() {
            @Override
            public <T extends Field> T createField(Class<?> dataType, Class<T> fieldType) {
                if (Set.class.equals(dataType)) {
                    OptionGroup f = new OptionGroup();
                    f.setMultiSelect(true);
                    for (Role r : ((UsersHelper)getHelper()).getRoles()) {
                        f.addItem(r);
                        f.setItemCaption(r, r.getName());
                    }
                    return (T) f;
                } else {
                    return super.createField(dataType, fieldType);
                }
            }
        };
    }


    @Override
    public void showSpecificView (String parameter) {
        forcePasswordChange = parameter.contains("password_change");
        super.showSpecificView(parameter);
        if (forcePasswordChange && passwordFieldGroup != null) {
            getEditBtn().click();
            passwordFieldGroup.setReadOnly(false);
            passwordFieldGroup.getField("current").focus();
            changePassBtn.setEnabled(false);
            getCancelBtn().setEnabled(false);
            getApp().scrollIntoView(passwordPanel);
        }

    }

    @Override
    public void setGridGetters() {
        Grid<User> g = getGrid();
        g.addColumn(User::getId).setId("id");
        g.addColumn(User::getName).setId("name");
        g.addColumn(User::getNick).setId("nick");
        g.addColumn(User::getEmail).setId("email");
        g.addColumn(User::isActive).setId("active");
        g.addColumn(User::isDeleted).setId("deleted");
        g.addColumn(User::isVerified).setId("verified");
        g.addColumn(User::getStartDate).setId("startDate");
        g.addColumn(User::getEndDate).setId("endDate");
        g.addColumn(User::isForcePasswordChange).setId("forcePasswordChange");
        g.addColumn(User::getLastLogin).setId("lastLogin");
        g.addColumn(User::getPasswordChanged).setId("passwordChanged");
        g.addColumn(User::getLoginAttempts).setId("loginAttempts");

    }

    private Button createChangePasswordButton () {
        Button b = new Button(getApp().getMessage("changePassword"));
        b.setIcon(FontAwesome.LOCK);
        b.setStyleName(ValoTheme.BUTTON_LINK);
        b.addStyleName(ValoTheme.BUTTON_SMALL);
        b.setEnabled(false);
        b.addClickListener((Button.ClickListener) event -> {
            passwordPanel.setVisible(!passwordPanel.isVisible());
            passwordFieldGroup.setReadOnly(!passwordFieldGroup.isReadOnly());
            changePassBtn.setCaption(passwordPanel.isVisible() ?
                    getApp().getMessage("cancel") : getApp().getMessage("changePassword"));
        });
        return b;
    }

    private Button createResetPasswordButton () {
        Button b = new Button(getApp().getMessage("resetPassword"));
        b.setStyleName(ValoTheme.BUTTON_LINK);
        b.addStyleName(ValoTheme.BUTTON_SMALL);
        b.setEnabled(false);
        b.addClickListener((Button.ClickListener) event -> resetPasswordClick());
        return b;
    }

    private void resetPasswordClick () {
        String generated = ((UsersHelper)getHelper()).resetUserPassword(getInstance());
        showGeneratedPassword(generated);
    }

    private void showGeneratedPassword (String generatedPassword) {
        String info = "<strong>" + generatedPassword + "</strong>";
        getApp().addWindow(new InfoDialog(getApp().getMessage("resetPasswordTitle"), info));
    }

    private Panel createPasswordPanel () {
        passwordPanel = new Panel(getApp().getMessage("changePassword"));
        passwordPanel.setIcon(FontAwesome.LOCK);
        passwordPanel.addStyleName("color1");
        passwordPanel.addStyleName("margin-top-panel");

        VerticalLayout panelContent = new VerticalLayout();
        panelContent.setSizeFull();
        panelContent.setMargin(true);
        panelContent.setSpacing(true);

        FormLayout form = new FormLayout();
        form.setSizeFull();
        panelContent.addComponent(form);
        panelContent.setExpandRatio(form, 1f);

        passwordFieldGroup = new FieldGroup();
        PropertysetItem passwordItem = new PropertysetItem();
        passwordItem.addItemProperty("current", new ObjectProperty<>(""));
        passwordItem.addItemProperty("new", new ObjectProperty<>(""));
        passwordItem.addItemProperty("repeat", new ObjectProperty<>(""));
        passwordFieldGroup.setItemDataSource(passwordItem);
        passwordFieldGroup.setReadOnly(true);

        if (selectedU.getId() != null) {
            PasswordField currentPass = new PasswordField(getApp().getMessage("passwordForm.currentPassword"));
            currentPass.setWidth("80%");
            currentPass.setRequired(true);
            currentPass.setRequiredError(getApp().getMessage("errorMessage.req", currentPass.getCaption()));
            currentPass.addValidator(((UsersHelper)getHelper()).getCurrentPasswordMatchValidator(selectedU, currentPass));
            currentPass.setImmediate(false);
            form.addComponent(currentPass);
            passwordFieldGroup.bind(currentPass, "current");
        }

        PasswordField newPass = new PasswordField(getApp().getMessage("passwordForm.newPassword"));
        newPass.setWidth("80%");
        newPass.setRequired(true);
        newPass.setRequiredError(getApp().getMessage("errorMessage.req",newPass.getCaption()));
        newPass.setImmediate(false);
        form.addComponent(newPass);

        PasswordField repeatPass = new PasswordField(getApp().getMessage("passwordForm.confirmPassword"));
        repeatPass.setWidth("80%");
        repeatPass.setImmediate(false);
        repeatPass.setRequired(true);
        repeatPass.setRequiredError(getApp().getMessage("errorMessage.req", repeatPass.getCaption()));
        form.addComponent(repeatPass);
        //Add validators
        newPass.addValidator(((UsersHelper)getHelper()).getNewPasswordNotUsedValidator(selectedU, newPass));
        repeatPass.addValidator(((UsersHelper)getHelper()).getPasswordsMatchValidator(newPass));

        passwordFieldGroup.bind(newPass, "new");
        passwordFieldGroup.bind(repeatPass, "repeat");
        passwordPanel.setVisible(forcePasswordChange);

        passwordPanel.setContent(panelContent);
        return passwordPanel;

    }

    private void hidePasswordPanel () {
        if (changePassBtn != null && passwordPanel != null) {
            changePassBtn.setCaption(getApp().getMessage("changePassword"));
            passwordPanel.setVisible(false);
            passwordFieldGroup.setReadOnly(true);
        }
    }

    protected void cancelClick (Button.ClickEvent event, Layout formLayout) {
        super.cancelClick (event, formLayout);
        if (changePassBtn != null) {
            changePassBtn.setEnabled(false);
            hidePasswordPanel();
        }
        if (resetPassBtn != null)
            resetPassBtn.setEnabled(false);
    }

    protected boolean saveClick (Button.ClickEvent event, Layout formLayout) {
        if (passwordFieldGroup != null && !passwordFieldGroup.isReadOnly()) {
            try {
                passwordFieldGroup.commit();
            } catch (FieldGroup.CommitException e) {
                for (Field f : e.getInvalidFields().keySet()) {
                    getErrorLabel().setValue(e.getInvalidFields().get(f).getMessage());
                    getErrorLabel().setVisible(true);
                }
                return false;
            }
        }
        if (super.saveClick (event, formLayout)) {
            if (resetPassBtn != null)
                resetPassBtn.setEnabled(false);
            if (changePassBtn != null) {
                passwordFieldGroup.clear();
                changePassBtn.setEnabled(false);
                hidePasswordPanel();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void editClick (Button.ClickEvent event, Layout formLayout) {
        super.editClick (event, formLayout);
        if (resetPassBtn != null)
            resetPassBtn.setEnabled(true);
        if (changePassBtn != null)
            changePassBtn.setEnabled(true);
    }

    @Override
    protected void addFields(Layout l) {
        selectedU = getBinder().getBean();
        //done separately because needs extra validator.
        TextField email = buildAndBindTextField("email");
        getBinder().forField(email).withValidator(new EmailValidator(getApp().getMessage("errorMessage.invalidEmail")));
//        TextField passwordChanged = new TextField("passwordChanged");
//        getBinder().forField(passwordChanged).withConverter(converter -> passwordChanged.getValue(),
//                converter2 -> passwordChanged.getValue()
//        ).bind("passwordChanged");


        l.addComponents(buildAndBindLongField("id"),buildAndBindTextField("name"),buildAndBindTextField("nick"),email,
                buildAndBindTextField("email"),buildAndBindBooleanField("active"),buildAndBindBooleanField("deleted")
                ,buildAndBindBooleanField("verified"),buildAndBindBooleanField("forcePasswordChange"),
                buildAndBindDateField("startDate"), buildAndBindDateField("endDate")
                ,buildAndBindLongField("loginAttempts"));
//        email.setWidth("60%");
//
//        nick.setRequired(true);
//        nick.setRequiredError(getApp().getMessage("errorMessage.req",nick.getCaption()));
//        Validator nickTakenV = ((UsersHelper) getHelper()).getNickTakenValidator(selectedU);
//        nick.addValidator(nickTakenV);
//        nick.setWidth("30%");
//        nick.setMaxLength(64);
//
//        name.setRequired(true);
//        name.setRequiredError(getApp().getMessage("errorMessage.req",name.getCaption()));
//        name.setWidth("60%");
//
        if (getBinder().getBean().getId().equals(getApp().getUser().getId())) {
            changePassBtn = createChangePasswordButton();
            l.addComponents(changePassBtn, createPasswordPanel());
        }
        if (getApp().getUser().hasPermission("sysadmin") && !isNewView()) {
            resetPassBtn = createResetPasswordButton();
            l.addComponent(resetPassBtn);
        }
//
//        return l;
    }

    @Override
    public boolean canEdit() {
        return true;
    }

    @Override
    public boolean canAdd() {
        return true;
    }

    @Override
    public boolean canRemove() {
        return true;
    }

}
