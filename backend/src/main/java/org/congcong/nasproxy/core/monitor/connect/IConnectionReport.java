package org.congcong.nasproxy.core.monitor.connect;

import org.congcong.nasproxy.common.entity.AttackLog;

public interface IConnectionReport {

    void reportAccessIp(String ip);

    void reportSuspiciousIp(String ip);

    void reportAttackLog(AttackLog attackLog);

}
