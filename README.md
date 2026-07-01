# Sistema de Mensagens Cliente-Servidor (EP2 & EP3)

Este projeto implementa um sistema completo de troca de mensagens e gerenciamento de contas utilizando arquitetura Cliente-Servidor com Sockets TCP em Java. A aplicação conta com interface gráfica (GUI) desenvolvida em Swing e utiliza um protocolo estrito baseado em JSON para a comunicação.

Projeto desenvolvido para a Universidade Tecnológica Federal do Paraná (UTFPR).

## 🚀 Funcionalidades

### Gerenciamento de Usuários (EP2)
* **Autenticação:** Login, Logout e Cadastro de novos usuários (com validação estrita de tamanho e caracteres para senhas e usernames).
* **Usuário Comum:** Consulta, atualização de dados cadastrais e exclusão da própria conta.
* **Administrador:** Visualização de todos os usuários cadastrados, atualização e deleção de qualquer conta-alvo. (A exclusão de um usuário ativo força a desconexão do seu Socket e o retorno à tela inicial).

### Chat em Tempo Real (EP3)
* **Lista de Online:** Atualização dinâmica dos usuários conectados na sessão.
* **Mensagem Privada:** Envio de mensagens diretas entre clientes conectados (roteamento Peer-to-Server-to-Peer).
* **Broadcast (`/todos`):** Envio de mensagens globais para todos os usuários do servidor simultaneamente.

## 🛠️ Tecnologias e Arquitetura
* **Linguagem:** Java
* **Interface Gráfica:** Java Swing
* **Comunicação de Rede:** Sockets TCP
* **Arquitetura do Servidor:** Servidor **Multi-Thread**. Cada cliente conectado ganha uma *Thread* dedicada (`TratadorCliente`) para evitar o bloqueio do servidor durante o tráfego de rede.
* **Estruturas de Dados:** Uso de `ConcurrentHashMap` para garantir *Thread-Safety* no banco de memória, prevenindo condições de corrida e inconsistências de concorrência.
* **Serialização de Dados:** JSON, utilizando a biblioteca [Gson](https://github.com/google/gson).
* **Build / Gerenciamento:** Maven (configuração via `pom.xml`).

## 📜 Protocolo de Comunicação
Toda a comunicação rede-a-rede segue um padrão rigoroso e síncrono predefinido:
1. Toda mensagem é um objeto JSON codificado em **UTF-8** e formatado em uma única linha.
2. O delimitador final é obrigatoriamente o caractere de escape de quebra de linha (`\n`).
3. Cada requisição enviada pelo cliente contém uma chave `"op"` detalhando a operação desejada.
4. O servidor devolve uma chave `"resposta"` contendo o código HTTP-like (ex: `200` para sucesso, `401` para erro/falha).

*Exemplo de fluxo de envio de Mensagem Privada:*

**O Cliente envia a requisição:**
```json
{"op": "enviarMensagem", "token": "usr_exemplo", "destinatario": "alvo123", "mensagem": "Testando a conexão!"}
