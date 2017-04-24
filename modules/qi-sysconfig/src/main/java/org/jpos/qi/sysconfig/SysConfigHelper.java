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


    import com.vaadin.v7.data.fieldgroup.BeanFieldGroup;
import com.vaadin.v7.data.fieldgroup.FieldGroup;
import com.vaadin.v7.data.util.BeanItem;
import org.jpos.ee.BLException;
import org.jpos.ee.DB;
import org.jpos.ee.SysConfig;
import org.jpos.ee.SysConfigManager;
import org.jpos.qi.QI;
import org.jpos.qi.QIHelper;

import java.util.*;
import java.util.stream.Stream;

public class SysConfigHelper extends QIHelper {
    private String prefix;

    public SysConfigHelper (String prefix) {
        super(SysConfig.class);
        this.prefix = prefix;
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
            BLException, CloneNotSupportedException
    {
        BeanItem<SysConfig> old = fieldGroup.getItemDataSource();
        Object oldSysConfig = old.getBean().clone();
        try {
            fieldGroup.commit();
        } catch (FieldGroup.CommitException e) {
            e.printStackTrace();
        }
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
    public boolean saveEntity (Object entity) throws BLException {
        String id = ((SysConfig)entity).getId();
        id = prefix != null ? prefix + id : id;
        if (getSysConfig(id) == null) {
            final String finalId = id;
            try {
                return (boolean) DB.execWithTransaction((db) -> {
                    SysConfigManager mgr = new SysConfigManager(db,prefix);
                    mgr.put(((SysConfig)entity).getId(), ((SysConfig)entity).getValue());
                    addRevisionCreated(db, "SYSCONFIG", finalId);
                    return true;
                });
            } catch (Exception e) {
                QI.getQI().getLog().error(e);
                return false;
            }
        } else {
//            fieldGroup.getField("id").focus();
//            fieldGroup.getItemDataSource().getItemProperty("id").setValue(null);
            throw new BLException("SysConfig " + id + " already exists.");
        }
    }

    @Override
    public Stream getAll(int offset, int limit, Map<String, Boolean> orders) throws Exception {
        SysConfig[] configs = (SysConfig[]) DB.exec(db -> {
            SysConfigManager mgr = new SysConfigManager(db,prefix);
            return mgr.getAll(offset,limit,orders);
        });
        return Arrays.asList(configs).stream();
    }

    @Override
    public int getItemCount() throws Exception {
        return (int) DB.exec(db -> {
            SysConfigManager mgr = new SysConfigManager(db,prefix);
            return mgr.getItemCount();
        });
    }

    @Override
    public String getItemId(Object item) {
        return ((SysConfig) item).getId();
    }

}
