package com.example.lab.wearsensor;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.util.Date;

@Entity
public class Record {
  @Id
  private Long id;

    @NotNull
    private double tempValue;
    private Date date;
    @Generated(hash = 1691249817)
    public Record(Long id, double tempValue, Date date) {
        this.id = id;
        this.tempValue = tempValue;
        this.date = date;
    }
    @Generated(hash = 477726293)
    public Record() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public double getTempValue() {
        return this.tempValue;
    }
    public void setTempValue(double tempValue) {
        this.tempValue = tempValue;
    }
    public Date getDate() {
        return this.date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
}
