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

                    // ==========================================
                    //  OPERAÇÕES DE AUTENTICAÇÃO E USUÁRIO
                    // ==========================================
                    
                    if (op.equals("login")) {
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();
                        Perfil p = banco.fazerLogin(u, s);
                        if (p != null) {
                            usuarioLogadoNaSessao = u;
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("token", p.getToken());
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Usuário ou senha inválidos");
                        }
                    } 
                    else if (op.equals("logout")) {
                        usuarioLogadoNaSessao = null;
                        resposta.addProperty("resposta", "200");
                        resposta.addProperty("mensagem", "logout efetuado");
                        out.println(resposta.toString());
                        logger.accept("<- ENVIOU: " + resposta.toString());
                        break; 
                    }
                    else if (op.equals("cadastrarUsuario")) {
                        String n = req.get("nome").getAsString();
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();

                        if (banco.cadastrarUsuario(n, u, s)) {
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("mensagem", "Cadastrado com sucesso");
                            resposta.addProperty("token", "usr_" + u); 
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Mensagem do erro"); 
                        }
                    }
                    else if (op.equals("consultarUsuario")) {
                        String token = req.get("token").getAsString();
                        String donoDoToken = banco.buscarUsuarioPorToken(token);

                        if (donoDoToken != null) {
                            Perfil p = banco.buscarPerfil(donoDoToken);
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("nome", p.getNome());
                            resposta.addProperty("usuario", p.getUsuario());
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }
                    else if (op.equals("atualizarUsuario")) {
                        String token = req.get("token").getAsString();
                        String n = req.get("nome").getAsString();
                        String s = req.get("senha").getAsString(); 
                        String donoDoToken = banco.buscarUsuarioPorToken(token);

                        if (donoDoToken != null) {
                            if (banco.atualizarUsuario(donoDoToken, n, s)) {
                                resposta.addProperty("resposta", "200");
                                resposta.addProperty("mensagem", "Atualizado com sucesso");
                            } else {
                                resposta.addProperty("resposta", "401");
                                resposta.addProperty("mensagem", "Mensagem do erro");
                            }
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }
                    else if (op.equals("deletarUsuario")) {
                        String token = req.get("token").getAsString();
                        String donoDoToken = banco.buscarUsuarioPorToken(token);
                        
                        if (donoDoToken != null) {
                            Perfil pDono = banco.buscarPerfil(donoDoToken);
                            
                            if (req.has("usuarioAlvo") && !req.get("usuarioAlvo").getAsString().isEmpty()) {
                                String alvo = req.get("usuarioAlvo").getAsString();
                                if (token.equals("adm") || (pDono != null && "ADMIN".equals(pDono.getNivel()))) {
                                    if (banco.apagarUsuario(alvo)) {
                                        resposta.addProperty("resposta", "200");
                                        resposta.addProperty("mensagem", "Deletado com sucesso");
                                    } else {
                                        resposta.addProperty("resposta", "401");
                                        resposta.addProperty("mensagem", "Mensagem do erro");
                                    }
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "Acesso Negado");
                                }
                            } 
                            else {
                                if (banco.apagarUsuario(donoDoToken)) {
                                    resposta.addProperty("resposta", "200");
                                    resposta.addProperty("mensagem", "Deletado com sucesso");
                                    out.println(resposta.toString());
                                    logger.accept("<- ENVIOU: " + resposta.toString());
                                    break; 
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "Mensagem do erro");
                                }
                            }
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }

                    // ==========================================
                    //  OPERAÇÕES DE MENSAGEM (CHAT) - LIMPO!
                    // ==========================================
                    else if (op.equals("enviarMensagem")) {
                        String token = req.get("token").getAsString();
                        String mensagemTexto = req.get("mensagem").getAsString(); 
                        String remetente = banco.buscarUsuarioPorToken(token);

                        if (remetente != null) {
                            JsonObject msgDTO = new JsonObject();
                            msgDTO.addProperty("usuario", remetente);
                            msgDTO.addProperty("mensagem", mensagemTexto); 
                            muralDeMensagens.add(msgDTO);

                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("mensagem", "Enviado com sucesso");
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }
                    else if (op.equals("receberMensagens")) { 
                        String token = req.get("token").getAsString();
                        if (banco.buscarUsuarioPorToken(token) != null) {
                            resposta.addProperty("resposta", "200");
                            resposta.add("mensagens", gson.toJsonTree(muralDeMensagens)); 
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }
                    else {
                        resposta.addProperty("resposta", "400");
                        resposta.addProperty("mensagem", "Operação não reconhecida");
                    }

                } catch (Exception e) {
                    resposta.addProperty("resposta", "401");
                    resposta.addProperty("mensagem", "Erro de formatação JSON");
                }
                
                if (resposta.has("resposta") && (!resposta.has("mensagem") || !resposta.get("mensagem").getAsString().equals("logout efetuado"))) {
                    out.println(resposta.toString());
                    logger.accept("<- ENVIOU: " + resposta.toString());
                }
            }
        } catch (IOException e) {
            logger.accept("Conexão com cliente encerrada ou rompida.");
        }
    }
}