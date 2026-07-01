package utf.servidor;

import java.io.*;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;



//Estou usando broadcast pq foi desse jeito que vc escreveu. Nao gosto muito do termo! Da para usar chat geral...
public class TratadorCliente implements Runnable {
    
    private Socket socket;
    private BancoDeDados banco;
    private ServidorGUI gui;
    private Gson gson = new Gson();
    private String usuarioLogadoNaSessao = null; 

    public TratadorCliente(Socket socket, BancoDeDados banco, ServidorGUI gui) {
        this.socket = socket;
        this.banco = banco;
        this.gui = gui;
    }
    
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {
            
            String msgRecebida;
            while ((msgRecebida = in.readLine()) != null) {
                gui.log("-> RECEBEU: " + msgRecebida);
                JsonObject resposta = new JsonObject();
                
                try {
                    JsonObject req = gson.fromJson(msgRecebida, JsonObject.class);
                    if (!req.has("op")) continue;

                    String op = req.get("op").getAsString();
                    String token = req.has("token") ? req.get("token").getAsString() : null;

                    // --- ACESSO ---
                    if (op.equals("login")){
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();
                        String ip = socket.getInetAddress().getHostAddress();
                        
                        Perfil p = banco.fazerLogin(u, s, ip);
                        if(p != null){
                            usuarioLogadoNaSessao = u;
                            gui.registrarClienteAtivo(u, out, socket); // Passa o socket agora
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("token", p.getToken());
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Usuário ou senha inválidos");
                        }
                    } 
                    else if(op.equals("logout")){
                        if(token != null && banco.fazerLogout(token)){
                            gui.removerClienteAtivo(usuarioLogadoNaSessao);
                            usuarioLogadoNaSessao = null;
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("mensagem", "logout efetuado");
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Erro ao efetuar logout");
                        }
                    }
                    else if(op.equals("cadastrarUsuario")){
                        String n = req.get("nome").getAsString();
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();
                        if(banco.cadastrarUsuario(n, u, s)){
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("mensagem", "Cadastrado com sucesso");
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Mensagem do erro"); 
                        }
                    }
                    //CONTA 
                    else if(op.equals("consultarUsuario")){
                        String dono = banco.buscarUsuarioPorToken(token);
                        if(dono != null){
                            Perfil p = banco.buscarPerfil(dono);
                            resposta.addProperty("resposta", "200");
                            resposta.addProperty("nome", p.getNome());
                            resposta.addProperty("usuario", p.getUsuario());
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }
                    else if(op.equals("atualizarUsuario")){
                        String n = req.get("nome").getAsString();
                        String s = req.get("senha").getAsString(); 
                        String dono = banco.buscarUsuarioPorToken(token);
                        if(dono != null){
                            if(banco.atualizarUsuario(dono, n, s)){
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
                    else if(op.equals("deletarUsuario")){
                        String dono = banco.buscarUsuarioPorToken(token);
                        if (dono != null){
                            if (banco.apagarUsuario(dono)){
                                resposta.addProperty("resposta", "200");
                                resposta.addProperty("mensagem", "Deletado com sucesso");
                                gui.removerClienteAtivo(usuarioLogadoNaSessao);
                                usuarioLogadoNaSessao = null;
                            } else {
                                resposta.addProperty("resposta", "401");
                                resposta.addProperty("mensagem", "Mensagem do erro");
                            }
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Token inválido");
                        }
                    }
                    //ADMIN
                    else if(op.equals("consultarUsuariosAdmin") || op.equals("consultarUsuarioAdmin") || op.equals("atualizarUsuarioAdmin") || op.equals("deletarUsuarioAdmin")){
                        String donoAdmin = banco.buscarUsuarioPorToken(token);
                        Perfil perfilAdmin = donoAdmin != null ? banco.buscarPerfil(donoAdmin) : null;

                        if(perfilAdmin != null && perfilAdmin.getNivel().equals("ADMIN")){
                            if(op.equals("consultarUsuariosAdmin")){
                                resposta.addProperty("resposta", "200");
                                resposta.add("lista_usuarios", gson.toJsonTree(banco.listarUsuariosParaAdmin()));
                            }
                            else if(op.equals("consultarUsuarioAdmin")){
                                String alvo = req.get("usuario").getAsString();
                                Perfil pAlvo = banco.buscarPerfil(alvo);
                                if(pAlvo != null){
                                    resposta.addProperty("resposta", "200");
                                    resposta.addProperty("nome", pAlvo.getNome());
                                    resposta.addProperty("usuario", pAlvo.getUsuario());
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "Token Inválido");
                                }
                            }
                            else if(op.equals("atualizarUsuarioAdmin")){
                                String alvo = req.get("usuario").getAsString();
                                String n = (req.has("nome") && !req.get("nome").isJsonNull()) ? req.get("nome").getAsString() : null;
                                String s = (req.has("senha") && !req.get("senha").isJsonNull()) ? req.get("senha").getAsString() : null;
                                
                                if(banco.atualizarUsuarioAdmin(alvo, n, s)){
                                    resposta.addProperty("resposta", "200");
                                    resposta.addProperty("mensagem", "Usuario atualizado com sucesso");
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "Mensagem de erro específico");
                                }
                            }
                            else if(op.equals("deletarUsuarioAdmin")){
                                String alvo = req.get("usuario").getAsString();
                                if(banco.apagarUsuarioAdmin(alvo)){
                                    resposta.addProperty("resposta", "200");
                                    resposta.addProperty("mensagem", "Usuario deletado com sucesso");
                                    gui.forcarDesconexao(alvo); // <--- ISSO FECHA O SOCKET DO ALVO
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "mensagem de erro específica");
                                }
                            }
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "Deve ser ADM para consultar a lista");
                        }
                    }
                    //CHAT
                    else if (op.equals("listarUsuariosLogados")) {
                        if (token != null && banco.buscarUsuarioPorToken(token) != null) {
                            resposta.addProperty("resposta", "200");
                            JsonArray array = new JsonArray();
                            for (String onlineUser : gui.getUsuariosOnline()) {
                                array.add(new JsonPrimitive(onlineUser));
                            }
                            resposta.add("lista_usuarios", array); 
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "mensagem de erro específica");
                        }
                    }
                    else if (op.equals("enviarMensagem")) {
                        String dono = banco.buscarUsuarioPorToken(token);
                        if (dono != null) {
                            String destinatario = req.get("destinatario").getAsString();
                            String mensagemTexto = req.get("mensagem").getAsString();

                            JsonObject msgParaEnviar = new JsonObject();
                            msgParaEnviar.addProperty("op", "receberMensagem");
                            msgParaEnviar.addProperty("remetente", dono);
                            msgParaEnviar.addProperty("mensagem", mensagemTexto);

                            if (destinatario.equals("/todos")) {
                                for (String onlineUser : gui.getUsuariosOnline()) {
                                    if (!onlineUser.equals(dono)) { 
                                        PrintWriter outDest = gui.getStreamDestinatario(onlineUser);
                                        if (outDest != null) outDest.println(msgParaEnviar.toString());
                                    }
                                }
                                resposta.addProperty("resposta", "200");
                                resposta.addProperty("mensagem", "Broadcast enviado");
                            } else {
                                PrintWriter outDest = gui.getStreamDestinatario(destinatario);
                                if (outDest != null) {
                                    outDest.println(msgParaEnviar.toString());
                                    resposta.addProperty("resposta", "200");
                                    resposta.addProperty("mensagem", "Mensagem enviada");
                                } else {
                                    resposta.addProperty("resposta", "401");
                                    resposta.addProperty("mensagem", "mensagem de erro específica");
                                }
                            }
                        } else {
                            resposta.addProperty("resposta", "401");
                            resposta.addProperty("mensagem", "mensagem de erro específica");
                        }
                    }

                } catch(Exception e){
                    resposta.addProperty("resposta", "401");
                    resposta.addProperty("mensagem", "Erro interno de JSON");
                }
                
                if(resposta.has("resposta")){
                    out.println(resposta.toString());
                    gui.log("<- ENVIOU: " + resposta.toString());
                }
            }
        } catch (IOException e) {
            gui.log("Conexão com cliente encerrada.");
        } finally {
            if (usuarioLogadoNaSessao != null) {
                gui.removerClienteAtivo(usuarioLogadoNaSessao);
                Perfil p = banco.buscarPerfil(usuarioLogadoNaSessao);
                if(p != null) p.limparSessao();
            }
            try { socket.close(); } catch(IOException e) {}
        }
    }
}