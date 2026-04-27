package utf.servidor;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class TratadorCliente {
    
    private Socket socket;
    private BancoDeDados banco;
    private Consumer<String> logger;
    private Gson gson = new Gson();

    private static List<JsonObject> muralDeMensagens = new ArrayList<>();
    private String usuarioLogadoNaSessao = null; 

    public TratadorCliente(Socket socket, BancoDeDados banco, Consumer<String> logger) {
        this.socket = socket;
        this.banco = banco;
        this.logger = logger;
    }

    public void processar() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            String msgRecebida;
            
            while ((msgRecebida = in.readLine()) != null) {
                logger.accept("-> RECEBEU: " + msgRecebida);
                JsonObject resposta = new JsonObject();
                
                try {
                    JsonObject req = gson.fromJson(msgRecebida, JsonObject.class);
                    if (!req.has("op")) continue;

                    String op = req.get("op").getAsString();

                    if (op.equals("login")) {
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();
                        Perfil p = banco.fazerLogin(u, s);
                        if (p != null) {
                            usuarioLogadoNaSessao = u;
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("token", p.getToken());
                            resposta.addProperty("mensagem", "Login ok");
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Erro no login");
                        }
                    } 
                    else if (op.equals("cadastrarUsuario")) {
                        String n = req.get("nome").getAsString();
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();

                        if (banco.cadastrarUsuario(n, u, s)) {
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("mensagem", "Cadastrado com sucesso");
                            
                            // AQUI O TOKEN APARECE PARA O CLIENTE:
                            resposta.addProperty("token", "usr_" + u); 
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Usuário já existe");
                        }
                    }
                    else if (op.equals("enviarMensagem")) {
                        String token = req.get("token").getAsString();
                        String texto = req.get("texto").getAsString();
                        String remetente = banco.buscarUsuarioPorToken(token);

                        if (remetente != null) {
                            JsonObject msgDTO = new JsonObject();
                            msgDTO.addProperty("remetente", remetente);
                            msgDTO.addProperty("texto", texto);
                            muralDeMensagens.add(msgDTO);

                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("mensagem", "Postado");
                        } else {
                            resposta.addProperty("resposta", "401");
                        }
                    }
                    else if (op.equals("read")) {
                        String token = req.get("token").getAsString();
                        if (banco.buscarUsuarioPorToken(token) != null) {
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("tipo", "mural");
                            resposta.add("lista", gson.toJsonTree(muralDeMensagens));
                        } else {
                            resposta.addProperty("resposta", "401");
                        }
                    }
                 // --- OPERAÇÃO: DELETAR UTILIZADOR (ADMIN OU AUTO) ---
                    else if (op.equals("deletarUsuario")) {
                        String token = req.get("token").getAsString();
                        String donoDoToken = banco.buscarUsuarioPorToken(token);
                        
                        if (donoDoToken != null) {
                            Perfil pDono = banco.buscarPerfil(donoDoToken);
                            
                            // Caso 1: O Admin quer apagar outra pessoa
                            if (req.has("usuarioAlvo")) {
                                String alvo = req.get("usuarioAlvo").getAsString();
                                
                                if (pDono.getNivel().equals("ADMIN")) {
                                    if (banco.apagarUsuario(alvo)) {
                                        resposta.addProperty("resposta", "200");
                                        resposta.addProperty("mensagem", "Utilizador [" + alvo + "] removido pelo Admin.");
                                    } else {
                                        resposta.addProperty("resposta", "401");
                                        resposta.addProperty("mensagem", "Erro: Alvo inexistente ou protegido.");
                                    }
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "Acesso Negado: Apenas administradores.");
                                }
                            } 
                            // Caso 2: O utilizador quer apagar a sua própria conta
                            else {
                                if (banco.apagarUsuario(donoDoToken)) {
                                    resposta.addProperty("resposta", "200");
                                    resposta.addProperty("mensagem", "Deslogado"); // Sinal para o cliente fechar
                                    
                                    // Envia a resposta final antes de quebrar o loop
                                    out.println(resposta.toString());
                                    logger.accept("<- ENVIOU: " + resposta.toString());
                                    break; 
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "O Admin principal nao pode ser removido.");
                                }
                            }
                        }
                    }
                    else if (op.equals("logout")) {
                        usuarioLogadoNaSessao = null;
                        resposta.addProperty("resposta", "200");
                        resposta.addProperty("mensagem", "Deslogado");
                        
                        out.println(resposta.toString());
                        logger.accept("<- ENVIOU: " + resposta.toString());
                        break; 
                    }
                    // --- TRAVA DE SEGURANÇA ---
                    else {
                        resposta.addProperty("resposta", "400");
                        resposta.addProperty("mensagem", "Operação desconhecida. Evitando travamento.");
                    }

                } catch (Exception e) {
                    resposta.addProperty("resposta", "401");
                    resposta.addProperty("mensagem", "Erro JSON");
                }
                
                if (resposta.has("resposta") && (!resposta.has("mensagem") || !resposta.get("mensagem").getAsString().equals("Deslogado"))) {
                    out.println(resposta.toString());
                    logger.accept("<- ENVIOU: " + resposta.toString());
                }
            }
        } catch (IOException e) {
            logger.accept("Conexão com cliente encerrada ou rompida.");
        }
    }
}