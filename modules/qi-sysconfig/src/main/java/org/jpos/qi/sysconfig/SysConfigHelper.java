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

package org.jpos.qi.sysconfig;


import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.fieldgroup.BeanFieldGroup;
import com.vaadin.v7.data.fieldgroup.FieldGroup;
import com.vaadin.v7.data.util.BeanItem;
import org.hibernate.criterion.Restrictions;
import org.jpos.ee.BLException;
import org.jpos.ee.DB;
import org.jpos.ee.SysConfig;
import org.jpos.ee.SysConfigManager;
import org.jpos.qi.EntityContainer;
import org.jpos.qi.QI;
import org.jpos.qi.QIHelper;

import java.util.*;

public class SysConfigHelper extends QIHelper {
    private String prefix;
    private SysConfigManager mgr;

    public SysConfigHelper (String prefix) {
        super(SysConfig.class);
        this.prefix = prefix;
        mgr = new SysConfigManager(prefix);
    }

    public Container createContainer() {
        Map<String, Class> properties = new LinkedHashMap<String, Class>();
        properties.put("id", String.class);
        properties.put("value", String.class);
        List sortable = Arrays.asList("id", "value");
        EntityContainer<SysConfig> ec = new EntityContainer<SysConfig>(SysConfig.class, properties, sortable);
        if(prefix != null)
            ec.addRestriction(Restrictions.like("id", prefix + "%"));
        return ec;
    }

    public SysConfig getSysConfig (String param) {
        try {
            return (SysConfig) DB.exec((db) -> db.session().get(SysConfig.class, param));
        } catch (Exception e) {
            QI.getQI().getLog().error(e);
            return null;
        }
    }

    @Override
    public boolean updateEntity (BeanFieldGroup fieldGroup) throws
            BLException, FieldGroup.CommitException, CloneNotSupportedException
    {
        BeanItem<SysConfig> old = fieldGroup.getItemDataSource();
        Object oldSysConfig = old.getBean().clone();
        fieldGroup.commit();
        BeanItem<SysConfig> item = fieldGroup.getItemDataSource();
        SysConfig s = item.getBean();
        try {
            return (boolean) DB.execWithTransaction((db) -> {
                db.session().merge(s);
                return addRevisionUpdated(db, getEntityName(),
                        String.valueOf(s.getId()),
                        oldSysConfig,
                        s,
                        new String[]{"id", "value"});
            });
        } catch (Exception e) {
            QI.getQI().getLog().error(e);
            return false;
        }
    }

    @Override
    public boolean saveEntity (BeanFieldGroup fieldGroup) throws FieldGroup.CommitException, BLException {
        fieldGroup.commit();
        BeanItem<SysConfig> item = fieldGroup.getItemDataSource();
        String id = (String) item.getItemProperty("id").getValue();
        id = prefix != null ? prefix + id : id;
        if (getSysConfig(id) == null) {
            final String finalId = id;
            try {
                return (boolean) DB.execWithTransaction((db) -> {
                    SysConfigManager mgr = new SysConfigManager(db,prefix);
                    mgr.put((String) item.getItemProperty("id").getValue(), (String) item.getItemProperty("value").getValue());
                    addRevisionCreated(db, "SYSCONFIG", finalId);
                    return true;
                });
            } catch (Exception e) {
                QI.getQI().getLog().error(e);
                return false;
            }
        } else {
            fieldGroup.getField("id").focus();
            fieldGroup.getItemDataSource().getItemProperty("id").setValue(null);
            throw new BLException("SysConfig " + id + " already exists.");
        }
    }

    @Override
    public DataProvider<SysConfig,Void> getDataProvider() {
        DataProvider<SysConfig, Void> dataProvider = DataProvider.fromCallbacks(
                (CallbackDataProvider.FetchCallback<SysConfig, Void>) query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    //return sysconfigManager.getSysConfigs(offset,limit);
                    try {
                        return Arrays.stream(mgr.getAll(offset, limit));
                    } catch (Exception e) {
                        getApp().getLog().error(e);
                        return null;
                    }
                },
                (CallbackDataProvider.CountCallback<SysConfig, Void>) query -> {
                    // return sysconfigManager.getSysConfigCount()
                    try {
                        return mgr.getItemsCount();
                    } catch (Exception e) {
                        getApp().getLog().error(e);
                        return 0;
                    }
                });
        return dataProvider;
    }



//    public boolean removeSysConfig (SysConfig sysConfig) {
//        return (boolean) DB.execWithTransaction((db) -> {
//            db.session().delete(sysConfig);
//            addRevisionRemoved(db, "SYSCONFIG", sysConfig.getId().toString());
//            return true;
//        });
//    }
}
