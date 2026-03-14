package com.gamifyapi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Exibe um quadro com links uteis no startup da aplicacao.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupLinksLogger {

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void exibirLinks() {
        String host = "localhost";
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        String baseUrl = "http://" + host + ":" + port + normalizarContexto(contextPath);

        String profileAtivo = obterPerfilAtivo();
        boolean isDev = "dev".equalsIgnoreCase(profileAtivo);

        String linha = "+------------------------------------------------------------------+";
        log.info("\n{}", linha);
        log.info("|                     GAMIFYAPI - LINKS UTEIS                     |");
        log.info("{}", linha);
        log.info("{}", formatarLinha("Ambiente", profileAtivo));
        log.info("{}", formatarLinha("API Base", baseUrl));
        log.info("{}", formatarLinha("Swagger", baseUrl + "/swagger-ui/index.html"));
        log.info("{}", formatarLinha("OpenAPI", baseUrl + "/v3/api-docs"));

        if (isDev) {
            log.info("{}", formatarLinha("H2 Console", baseUrl + "/h2-console"));
        }

        log.info("{}", linha);
    }

    private String normalizarContexto(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    private String obterPerfilAtivo() {
        String[] ativos = environment.getActiveProfiles();
        if (ativos.length == 0) {
            return "default";
        }
        return ativos[0];
    }

    private String formatarLinha(String label, String valor) {
        String prefixo = String.format("| %-9s: ", label);
        int larguraValor = Math.max(0, 66 - prefixo.length() - 2);
        String truncado = valor.length() > larguraValor ? valor.substring(0, larguraValor) : valor;
        return prefixo + String.format("%-" + larguraValor + "s", truncado) + " |";
    }
}
