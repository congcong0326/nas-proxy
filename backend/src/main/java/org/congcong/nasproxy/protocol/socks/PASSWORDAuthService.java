package org.congcong.nasproxy.protocol.socks;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PASSWORDAuthService {


    //private static ServiceConfig config;

    public static boolean checkUP(String username, String password, int serverPort) {
//        ServiceConfig config = ConfigProvider.getConfig();
//        for (ServiceConfig.Service service : config.getServices()) {
//            if (service.getPort() == serverPort) {
//                List<ServiceConfig.Credentials> credentials = service.getCredentials();
//                for (ServiceConfig.Credentials credential : credentials) {
//                    if (Objects.equals(credential.getUsername(), username)
//                            && Objects.equals(credential.getPassword(), password)) {
//                        log.info("user {} auth to {} port success", username, serverPort);
//                        return true;
//                    }
//                }
//            }
//        }
//        log.error("user {} auth to {} port failed", username, serverPort);
//        return false;
        return true;
    }

}
