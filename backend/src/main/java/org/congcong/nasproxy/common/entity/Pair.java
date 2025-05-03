package org.congcong.nasproxy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<T1,T2> {

    private T1 fst;

    private T2 snd;

}
