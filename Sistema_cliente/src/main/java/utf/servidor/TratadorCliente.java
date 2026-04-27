package utf.servidor;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class TratadorCliente implements Runnable {
    
    private Socket socket;
    private BancoDeDados banco;
    private Consumer<String> logger;
    private Gson gson = new Gson();

    // MAPA GLOBAL: Fundamental para o chat 1-para-1 sem destinatário manual
    private static ConcurrentHashMap<String, PrintWriter> usuariosOnline = new ConcurrentHashMap<>();
    private String usuarioLogadoNaSessao = null; 

    public TratadorCliente(Socket socket, BancoDeDados banco, Consumer<String> logger) {
        this.socket = socket;
        this.banco = banco;
        this.logger = logger;
    }

    // Regras do Protocolo
    private boolean validarUsuario(String u) { return u != null && u.matches("^[a-zA-Z0-9]{5,20}$"); }
    private boolean validarSenha(String s) { return s != null && s.matches("^[0-9]{6}$"); }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            String mensagemRecebida;

            while ((mensagemRecebida = in.readLine()) != null) {
                logger.accept("-> RECEBEU: " + mensagemRecebida);
                JsonObject respostaServidor = new JsonObject();
                
                try {
                    JsonObject req = gson.fromJson(mensagemRecebida, JsonObject.class);
                    if (!req.has("op")) continue;

                    String op = req.get("op").getAsString();

                    // --- 1. LOGIN ---
                    if (op.equals("login")) {
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();
                        Perfil p = banco.fazerLogin(u, s);
                        
                        if (p != null) {
                            usuarioLogadoNaSessao = u;
                            usuariosOnline.put(u, out);
                            respostaServidor.addProperty("resposta", "200");
                            respostaServidor.addProperty("token", p.getToken());
                            respostaServidor.addProperty("mensagem", "Login efetuado");
                        } else {
                            respostaServidor.addProperty("resposta", "401");
                            respostaServidor.addProperty("mensagem", "Usuario ou senha incorretos");
                        }
                    } 
                    
                    // --- 2. CADASTRAR ---
                    else if (op.equals("cadastrarUsuario")) {
                        String n = req.get("nome").getAsString();
                        String u = req.get("usuario").getAsString();
                        String s = req.get("senha").getAsString();
                        
                        if (!validarUsuario(u) || !validarSenha(s)) {
                            respostaServidor.addProperty("resposta", "401");
                            respostaServidor.addProperty("mensagem", "Padrao invalido");
                        } else if (banco.cadastrarUsuario(n, u, s)) {
                            respostaServidor.addProperty("resposta", "200");
                            respostaServidor.addProperty("mensagem", "Cadastrado com sucesso");
                        } else {
                            respostaServidor.addProperty("resposta", "401");
                            respostaServidor.addProperty("mensagem", "Erro: Login ja existe");
                        }
                    }

                    // --- 3. ENVIAR MENSAGEM (SEM DESTINATÁRIO NO JSON) ---
                    else if (op.equals("enviarMensagem")) {
                        String token = req.get("token").getAsString();
                        String texto = req.get("texto").getAsString();
                        String remetente = banco.buscarUsuarioPorToken(token);

                        if (remetente != null) {
                            String amigoOnline = null;
                            
                            // Lógica de Roteamento: Pega qualquer um que NÃO seja quem enviou
                            for (String loginOnline : usuariosOnline.keySet()) {
                                if (!loginOnline.equals(remetente)) {
                                    amigoOnline = loginOnline;
                                    break;
                                }
                            }

                            if (amigoOnline != null) {
                                PrintWriter outAmigo = usuariosOnline.get(amigoOnline);
                                
                                JsonObject msgChat = new JsonObject();
                                msgChat.addProperty("op", "receberMensagem");
                                msgChat.addProperty("remetente", remetente);
                                msgChat.addProperty("texto", texto);
                                
                                outAmigo.println(msgChat.toString());
                                
                                // Resposta silenciosa para o remetente (não polui o chat dele)
                                respostaServidor.addProperty("resposta", "200");
                                respostaServidor.addProperty("mensagem", "Entregue"); 
                            } else {
                                respostaServidor.addProperty("resposta", "401");
                                respostaServidor.addProperty("mensagem", "Aguardando outra pessoa logar...");
                            }
                        }
                    }

                    // --- 4. CONSULTAR ---
                    else if (op.equals("consultarUsuario")) {
                        String token = req.get("token").getAsString();
                        String dono = banco.buscarUsuarioPorToken(token);
                        if (dono != null) {
                            Perfil p = banco.buscarPerfil(dono);
                            respostaServidor.addProperty("resposta", "200");
                            respostaServidor.addProperty("nome", p.getNome());
                            respostaServidor.addProperty("usuario", dono);
                            respostaServidor.addProperty(token, p.getToken());
                        }
                    }

                    // --- 5. ATUALIZAR ---
                    else if (op.equals("atualizarUsuario")) {
                        String token = req.get("token").getAsString();
                        String n = req.get("nome").getAsString();
                        String s = req.get("senha").getAsString();
                        String dono = banco.buscarUsuarioPorToken(token);
                        if (dono != null && validarSenha(s)) {
                            banco.atualizarUsuario(dono, n, s);
                            respostaServidor.addProperty("resposta", "200");
                            respostaServidor.addProperty("mensagem", "Dados atualizados");
                        }
                    }

                    // --- 6. DELETAR (Com Regra Admin) ---
                    else if (op.equals("deletarUsuario")) {
                        String token = req.get("token").getAsString();
                        String dono = banco.buscarUsuarioPorToken(token);
                        if (dono != null) {
                            // Se houver usuarioAlvo no JSON, é o Admin deletando outro
                            if (req.has("usuarioAlvo")) {
                                String alvo = req.get("usuarioAlvo").getAsString();
                                if (banco.buscarPerfil(dono).getNivel().equals("ADMIN")) {
                                    if (banco.apagarUsuario(alvo)) {
                                        usuariosOnline.remove(alvo);
                                        respostaServidor.addProperty("resposta", "200");
                                        respostaServidor.addProperty("mensagem", "Deletado pelo Admin");
                                    }
                                }
                            } else {
                                // Auto-deleção
                                if (banco.apagarUsuario(dono)) {
                                    usuariosOnline.remove(dono);
                                    usuarioLogadoNaSessao = null;
                                    respostaServidor.addProperty("resposta", "200");
                                } else {
                                    respostaServidor.addProperty("resposta", "401");
                                    respostaServidor.addProperty("mensagem", "Admin nao pode ser deletado");
                                }
                            }
                        }
                    }

                    // --- 7. LOGOUT ---
                    else if (op.equals("logout")) {
                        if (usuarioLogadoNaSessao != null) {
                            usuariosOnline.remove(usuarioLogadoNaSessao);
                            usuarioLogadoNaSessao = null;
                            respostaServidor.addProperty("resposta", "200");
                        }
                    }

                } catch (Exception e) {
                    respostaServidor.addProperty("resposta", "401");
                    respostaServidor.addProperty("mensagem", "Erro no processamento");
                }
                
                // Envia a resposta final da operação
                if (respostaServidor.has("resposta")) {
                    out.println(respostaServidor.toString());
                    logger.accept("<- ENVIOU: " + respostaServidor.toString());
                }
            }
        } catch (IOException e) {
            logger.accept("Conexão encerrada.");
        } finally {
            if (usuarioLogadoNaSessao != null) {
                usuariosOnline.remove(usuarioLogadoNaSessao);
            }
        }
    }
}