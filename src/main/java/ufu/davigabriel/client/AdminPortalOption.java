package ufu.davigabriel.client;

import lombok.Getter;

@Getter
public enum AdminPortalOption {
    NOOP,
    CRIAR_CLIENTE,
    BUSCAR_CLIENTE,
    MUDAR_CLIENTE,
    REMOVER_CLIENTE,
    CRIAR_PRODUTO,
    BUSCAR_PRODUTO,
    MUDAR_PRODUTO,
    REMOVER_PRODUTO,
    SAIR
}
