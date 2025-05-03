package org.congcong.nasproxy.common.entity;

import lombok.Getter;
import lombok.Setter;


public class Triple<T1,T2,T3> extends Pair <T1,T2>{

    @Getter
    @Setter
    private T3 third;

    public Triple(T1 fst, T2 snd, T3 third) {
        super(fst, snd);
        this.third = third;
    }
}
