package com.heterodain.gtimonitor.model;

import lombok.Data;

/**
 * 計測データ
 */
@Data
public class MeasureData {
    /** 照度(Lux) */
    private Double light;
    /** 電力(Wh) */
    private Double power;
    /** ハッシュレート(MH/s) */
    private Double hash;
}
