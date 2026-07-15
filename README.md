# HY300 Remote MVP

São dois APKs independentes na mesma rede local:

| Módulo / APK | Nome exibido | Instalação | Função |
| --- | --- | --- | --- |
| `client` / `client-debug.apk` | **HY300 Controller** | Celular Android | Touchpad, teclado, atalhos, descoberta e pareamento. |
| `server` / `server-debug.apk` | **HY300 Receiver** | Projetor HY300 | WebSocket, Accessibility Service, cursor virtual e Shizuku. |

## Estado deste repositório

Este é um MVP de validação de arquitetura, ainda não a versão final do produto. Ele implementa WebSocket, pareamento com token persistido e expiração de 30 dias, anúncio e descoberta NSD, heartbeat/reconexão no cliente, touchpad e cursor/gestos via Accessibility Service. A tela do cliente lista os projetores descobertos, mas preserva a conexão manual por IP:porta como fallback.

## Validação obrigatória no HY300

Antes de testar o fluxo completo:

1. Instale o `server`, abra-o e ative o serviço de acessibilidade em **Configurações > Acessibilidade**. Confirme que o cursor aparece e se move.
2. Em um PC, valide `adb shell input keyevent 3` e `adb shell input text teste` com um campo de texto aberto.
3. Confirme a existência de **Depuração sem fio**. Só então configure Shizuku e habilite a implementação da ponte.

Sem overlay, o app ainda pode enviar gestos, mas não há cursor visível. HOME/BACK usam uma ação global do serviço de acessibilidade. POWER, volume e eventos de tecla usam Shizuku quando autorizado. Texto usa `input text` via Shizuku e tem fallback `ACTION_SET_TEXT`, que só funciona quando o app alvo expõe campo editável.

## Limitações de validação

O CI compila os dois APKs a cada push. Não substitui os testes no projetor: execute os testes de overlay, ADB e Wireless Debugging antes de liberar o app para uso diário. A compatibilidade final de Shizuku e do overlay depende da ROM do HY300.

## Uso

1. Compile e instale `server` no projetor; conceda a permissão de notificações se solicitada e habilite a acessibilidade.
2. Abra o servidor: o código de seis dígitos e endereço aparecem na tela.
3. Instale/abra `client` no celular, escolha o projetor descoberto ou informe `IP:7300`, e use o código para parear.

O token fica em `DataStore` e expira depois de 30 dias. Há somente um cliente ativo por vez.
