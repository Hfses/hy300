# HY300 Remote MVP

Dois aplicativos Android na mesma rede local:

- `server`: instalar no HY300. Mantém o WebSocket, exibe um cursor virtual e despacha toques pelo Accessibility Service.
- `client`: instalar no telefone. Conecta por IP:porta, faz pareamento e oferece touchpad e atalhos.

## Estado deste repositório

Este é o MVP de validação de arquitetura. Ele implementa comunicação WebSocket, pareamento com token temporário em memória, touchpad e cursor/gestos via Accessibility Service. NSD, reconexão/heartbeat, teclado e Shizuku são a próxima etapa e não devem ser apresentados como prontos.

## Validação obrigatória no HY300

Antes de testar o fluxo completo:

1. Instale o `server`, abra-o e ative o serviço de acessibilidade em **Configurações > Acessibilidade**. Confirme que o cursor aparece e se move.
2. Em um PC, valide `adb shell input keyevent 3` e `adb shell input text teste` com um campo de texto aberto.
3. Confirme a existência de **Depuração sem fio**. Só então configure Shizuku e habilite a implementação da ponte.

Sem overlay, o app ainda pode enviar gestos, mas não há cursor visível. Sem ADB/Shizuku, HOME, POWER, volume e injeção de texto ficam fora do MVP sem privilégios.

## Uso

1. Compile e instale `server` no projetor; conceda a permissão de notificações se solicitada e habilite a acessibilidade.
2. Abra o servidor: o código de seis dígitos e endereço aparecem na tela.
3. Instale/abra `client` no celular, escolha o projetor descoberto ou informe `IP:7300`, e use o código para parear.

O token fica em `DataStore` e expira depois de 30 dias. Há somente um cliente ativo por vez.
