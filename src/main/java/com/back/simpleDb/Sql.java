package com.back.simpleDb;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    //return this로 체이닝
    public Sql append(String sqlPart) {
        query.append(" ").append(sqlPart);
        return this;
    }

    //params 구별
    public Sql append(String sqlPart, Object... params) {
        query.append(" ").append(sqlPart);
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }
    //IN (?)를 params 개수만큼 IN (?,?,?) 형태로 변경(SQL Injection 방지, 타입 안정성)
    public Sql appendIn(String sqlPart, Object... params) {
        query.append(" ").append(sqlPart.replace("?",
                String.join(",", Collections.nCopies(params.length, "?"))));
        Collections.addAll(this.params, params);
        return this;
    }

    //t001
    public long insert() { return simpleDb.runInsert(query.toString(), params.toArray()); }
    //t002
    public int update() { return simpleDb.runUpdate(query.toString(), params.toArray());}
    //t003
    public int delete() {
        return simpleDb.runDelete(query.toString(), params.toArray());
    }
    //t004
    public List<Map<String, Object>> selectRows() {
        return simpleDb.runSelectRows(query.toString(), params.toArray());
    }
    //t015
    public <T> List<T> selectRows(Class<T> cls) { return simpleDb.runSelectRows(cls, query.toString(), params.toArray());}
    //t005
    public Map<String, Object> selectRow() {
        return simpleDb.runSelectRow(query.toString(), params.toArray());
    }
    //t016
    public <T> T selectRow(Class<T> cls) { return simpleDb.runSelectRow(cls, query.toString(), params.toArray()); }
    //t006
    public LocalDateTime selectDatetime() { return simpleDb.runSelectDateTime(query.toString(), params.toArray()); }
    //t007, t012, t013
    public Long selectLong() { return simpleDb.runSelectLong(query.toString(), params.toArray());}
    //t008
    public String selectString() {
        return simpleDb.runSelectString(query.toString(), params.toArray());
    }
    //t009, t010, t011
    public Boolean selectBoolean() { return simpleDb.runSelectBoolean(query.toString(), params.toArray()); }
    //t014
    public List<Long> selectLongs() {  return simpleDb.runSelectLongs(query.toString(), params.toArray()); }

}
