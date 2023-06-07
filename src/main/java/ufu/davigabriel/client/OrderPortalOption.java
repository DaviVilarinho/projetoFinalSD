package ufu.davigabriel.client;

import lombok.Getter;

@Getter
public enum OrderPortalOption {
    NOOP,
    CRIAR_PEDIDO,
    BUSCAR_PEDIDO,
    MUDAR_PEDIDO,
    REMOVER_PEDIDO,
    MEUS_PEDIDOS,
    SAIR
}
