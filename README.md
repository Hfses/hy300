# HY300 Remote MVP

Dois aplicativos Android na mesma rede local:

- `server`: instalar no HY300. Mantém o WebSocket, exibe um cursor virtual e despacha toques pelo Accessibility Service.
- `client`: instalar no telefone. Conecta por IP:porta, faz pareamento e oferece touchpad e atalhos.

## Estado deste repositório

Este é um MVP de validação de arquitetura, ainda não a versão final do produto. Ele implementa WebSocket, pareamento com token persistido e expiração de 30 dias, anúncio NSD no servidor, heartbeat/reconexão no cliente, touchpad e cursor/gestos via Accessibility Service. O cliente ainda oferece conexão manual por IP:porta; a seleção automática de serviços NSD, Shizuku e gestos multi-toque precisam ser concluídos e testados no hardware.

## Validação obrigatória no HY300

Antes de testar o fluxo completo:

1. Instale o `server`, abra-o e ative o serviço de acessibilidade em **Configurações > Acessibilidade**. Confirme que o cursor aparece e se move.
2. Em um PC, valide `adb shell input keyevent 3` e `adb shell input text teste` com um campo de texto aberto.
3. Confirme a existência de **Depuração sem fio**. Só então configure Shizuku e habilite a implementação da ponte.

Sem overlay, o app ainda pode enviar gestos, mas não há cursor visível. HOME/BACK usam uma ação global do serviço de acessibilidade. POWER e volume continuam dependentes de Shizuku; texto possui um fallback via `ACTION_SET_TEXT`, que só funciona quando o app alvo expõe campo editável.

## Uso

1. Compile e instale `server` no projetor; conceda a permissão de notificações se solicitada e habilite a acessibilidade.
2. Abra o servidor: o código de seis dígitos e endereço aparecem na tela.
3. Instale/abra `client` no celular, escolha o projetor descoberto ou informe `IP:7300`, e use o código para parear.

O token fica em `DataStore` e expira depois de 30 dias. Há somente um cliente ativo por vez.
