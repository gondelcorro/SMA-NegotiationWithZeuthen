package Behaviour;

import Agent.AgentNegociator;
import Concepts.IsMyZeuthen;
import Concepts.Movie;
import Concepts.SeeMovie;
import Ontology.MCPOntology;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class FSM_Negotiation extends FSMBehaviour {

    IsMyZeuthen myZeuthen = new IsMyZeuthen();
    DataStore ds = new DataStore();

    //Constantes data store
    public static String PELICULAS_DISPONIBLES = "PeliculasDisponibles";
    public static String RECEIVER_AID = "ReceiverID";

    //Constantes Estados
    private static final String ENVIAR_PROPUESTA = "EnviarPropuesta";
    private static final String ESPERAR_RESPUESTA = "EsperarRespuesta";
    private static final String CALCULAR_Y_ENVIAR_ZEUTHEN = "CalcularYEnviarZeuthen";
    private static final String RECIBIR_ZEUTHEN_OPONENTE = "RecibirZeuthenOponente";
    private static final String ESPERAR_PROPUESTA = "EsperarPropuesta";
    private static final String EVALUAR_PROPUESTA_Y_RESPONDER = "EvaluarPropuestaYResponder";

    //State's transition values
    //public static int MI_ZEUTHEN_MAYOR = 1;
    //public static int MI_ZEUTHEN_MENOR = 5;

    public FSM_Negotiation(AID agentID, Boolean inicio, ArrayList<String> peliculasDisponibles) {

        ds.put(RECEIVER_AID, agentID); // ID "B"
        ds.put(PELICULAS_DISPONIBLES, peliculasDisponibles);
        System.out.println("*** Inicia Negociacion ***");
        if (inicio) {
            EsperarPropuesta s1 = new EsperarPropuesta();
            s1.setDataStore(ds);
            this.registerFirstState(s1, ESPERAR_PROPUESTA);
        } else {
            EnviarPropuesta s1 = new EnviarPropuesta();
            s1.setDataStore(ds);
            this.registerFirstState(s1, ENVIAR_PROPUESTA);
        }
        EsperarRespuesta s2 = new EsperarRespuesta();
        s2.setDataStore(ds);
        this.registerState(s2, ESPERAR_RESPUESTA);

        CalcularYEnviarZeuthen s3 = new CalcularYEnviarZeuthen();
        s3.setDataStore(ds);
        this.registerState(s3, CALCULAR_Y_ENVIAR_ZEUTHEN);

        RecibirZeuthenOponente s4 = new RecibirZeuthenOponente();
        s4.setDataStore(ds);
        this.registerState(s4, RECIBIR_ZEUTHEN_OPONENTE);

        EvaluarPropuestaYResponder s5 = new EvaluarPropuestaYResponder();
        s5.setDataStore(ds);
        this.registerState(s5, EVALUAR_PROPUESTA_Y_RESPONDER);

        this.registerDefaultTransition(ENVIAR_PROPUESTA, ESPERAR_RESPUESTA);
        this.registerDefaultTransition(ESPERAR_RESPUESTA, CALCULAR_Y_ENVIAR_ZEUTHEN);
        this.registerDefaultTransition(EVALUAR_PROPUESTA_Y_RESPONDER, CALCULAR_Y_ENVIAR_ZEUTHEN);
        this.registerDefaultTransition(CALCULAR_Y_ENVIAR_ZEUTHEN, RECIBIR_ZEUTHEN_OPONENTE);
        this.registerTransition(RECIBIR_ZEUTHEN_OPONENTE, ESPERAR_PROPUESTA, 5); //num de estado
        this.registerDefaultTransition(ESPERAR_PROPUESTA, EVALUAR_PROPUESTA_Y_RESPONDER);
        this.registerTransition(RECIBIR_ZEUTHEN_OPONENTE, ENVIAR_PROPUESTA, 1); //num de estado
    }

    private class EnviarPropuesta extends Behaviour {

        private Boolean envio = false;
        private Boolean primeraVez = true;

        @Override
        public void action() {
            AgentNegociator ag = ((AgentNegociator) myAgent);
            String sigPropuesta;
            if (primeraVez) {
                sigPropuesta = ag.getPropuestaActual();
            } else {
                sigPropuesta = ag.elegirPropuesta();
            }
            if (sigPropuesta == null) { // if == null => no hay más!
                System.out.println("NO HAY MAS PELICULAS PARA PROPONER");
            } else { //Arma el mensaje! y lo envia
                try {
                    Movie movie = new Movie();
                    movie.setName(sigPropuesta);
                    SeeMovie seeMovie = new SeeMovie();
                    seeMovie.setMovie(movie);
                    seeMovie.setDate(new Date(2018, 9, 17));
                    ACLMessage msgPropuesta = new ACLMessage(ACLMessage.PROPOSE);
                    msgPropuesta.addReceiver((AID) this.getDataStore().get(RECEIVER_AID));
                    msgPropuesta.setContentObject(seeMovie);
                    msgPropuesta.setLanguage(MCPOntology.getCodecInstance().getName());
                    msgPropuesta.setOntology(MCPOntology.getInstance().getName());
                    msgPropuesta.setConversationId("negociacion-pelicula");
                    msgPropuesta.setReplyWith("propuesta" + System.currentTimeMillis()); // valor unico
                    myAgent.send(msgPropuesta);
                    //MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociacion-pelicula"), MessageTemplate.MatchInReplyTo(msgPropuesta.getReplyWith()));
                    System.out.println("Agente " + myAgent.getLocalName() + ": Envia propuesta: " + movie.getName());
                    envio = true;
                    primeraVez = false;
                } catch (IOException ex) {
                    System.out.println("Error: " + ex);
                }
            }
        }

        @Override
        public boolean done() {
            return envio;
        }
    }

    private class EsperarRespuesta extends Behaviour {

        boolean respuesta = false;

        public void action() {
            ACLMessage msgRespuesta = myAgent.receive();
            if (msgRespuesta != null) {// respuesta recibida
                if (msgRespuesta.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta aceptada - FIN DE LA NEGOCIACION");
                    myAgent.doDelete();
                } else { //si rechazó
                    this.getDataStore().put("propuestaDelOtroAgente", msgRespuesta.getContent()); //Guardo la propuesta actual del otro agente en el DS
                    System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta rechazada... paso a informar zeuthen...");
                    this.getDataStore().put("msgRechazo", msgRespuesta); // El msj de respuesta es un rechazo, lo guardo
                    respuesta = true;
                }
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando respuesta..");
            }
        }

        @Override
        public boolean done() {
            return respuesta;
        }
    }

    private class CalcularYEnviarZeuthen extends Behaviour {

        private Boolean envioZeuthen = false;

        @Override
        public void action() {
            try {
                AgentNegociator agente = ((AgentNegociator) myAgent);
                float utilidadPropActual = agente.getUtilidad(agente.getPropuestaActual());
                String propuestaOponente = (String) this.getDataStore().get("propuestaDelOtroAgente");
                float utilidadPropuestaOponente = agente.getUtilidad(propuestaOponente);
                float zeuthen = (utilidadPropActual - utilidadPropuestaOponente) / utilidadPropActual;//Calculo el zeuthen
                this.getDataStore().put("miZeuthen", zeuthen);

                ACLMessage msgRechazo = (ACLMessage) this.getDataStore().get("msgRechazo");
                if (msgRechazo != null) { // B ESTABA ESPERANDO LA RESPUESTA.. CUANDO LLEGA EL MSJ DE RECHAZO INFORMA SU ZEUTHEN A A
                    System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es " + zeuthen);
                    ACLMessage msgZeuthen = msgRechazo.createReply();
                    msgZeuthen.setPerformative(ACLMessage.INFORM);
                    msgZeuthen.setContentObject(zeuthen);
                    envioZeuthen = true;
                    myAgent.send(msgZeuthen);
                } else { // A RECHAZÓ, B QUEDÓ ESPERANDO.. ENTONCS A VUELVE A ENVIAR EN ESTE CASO EL ZEUTHEN A B
                    System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es " + zeuthen );
                    ACLMessage propuesta = (ACLMessage) this.getDataStore().get("msgPropuesta");
                    //ACLMessage msgZeuthen = new ACLMessage(ACLMessage.INFORM);
                    //msgZeuthen.addReceiver((AID)ds.get(RECEIVER_AID));
                    ACLMessage msgZeuthen = propuesta.createReply();
                    //msgZeuthen.addReceiver(propuesta.getSender());
                    msgZeuthen.setPerformative(ACLMessage.INFORM);
                    msgZeuthen.setContentObject(zeuthen);
                    envioZeuthen = true;
                    myAgent.send(msgZeuthen);
                }
            } catch (IOException ex) {
                System.out.println("Error en el msje");
            }
        }

        @Override
        public boolean done() {
            return envioZeuthen;
        }
    }

    private class RecibirZeuthenOponente extends Behaviour {

        private int proxEstado;
        private boolean received = false;

        @Override
        public void action() {
            ACLMessage msgZeuthen = myAgent.receive();
            if (msgZeuthen != null) {
                try {
                    float zuethenOponente = (float) msgZeuthen.getContentObject();
                    float miZeuthen = (float) this.getDataStore().get(("miZeuthen"));
                    System.out.println("Agente " + myAgent.getLocalName() + ": recibe zeuthen oponente... " + "(" + zuethenOponente + ")");
                    if (miZeuthen > zuethenOponente) {
                        proxEstado = 1; // mi zeuthen es mayor -> enviar propuesta
                        received = true;
                        System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es mayor.. envio propuesta");
                    } else {
                        proxEstado = 5; // mi zeuthen es menor -> esperar propuesta
                        received = true;
                        System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es menor.. espero propuesta");
                    }
                } catch (UnreadableException ex) {
                    System.out.println("Error en el msje recibiendo zeuthen: " + ex);
                }
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando Zeuthen oponente...");
            }
        }

        @Override
        public int onEnd() {
            return proxEstado;
        }

        @Override
        public boolean done() {
            return received;
        }
    }

    private class EsperarPropuesta extends Behaviour {

        Boolean esperarPropuesta = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msgPropuesta = myAgent.receive(mt);
            if (msgPropuesta != null) {
                this.getDataStore().put("msgPropuesta", msgPropuesta);
                System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta recibida.. paso a evaluar propusta y responder...");
                esperarPropuesta = true;
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando propuesta...");
            }
        }

        @Override
        public boolean done() {
            return esperarPropuesta;
        }
    }

    private class EvaluarPropuestaYResponder extends Behaviour {

        private Boolean evaluarYResponder = false;

        @Override
        public void action() {
            ACLMessage propuesta = (ACLMessage) this.getDataStore().get("msgPropuesta");
            if (propuesta != null) {
                try {// Propuesta recibida
                    AgentNegociator agente = ((AgentNegociator) myAgent);
                    ACLMessage respuesta = propuesta.createReply();
                    SeeMovie seeMovie = (SeeMovie) propuesta.getContentObject();
                    String peliPropuesta = seeMovie.getMovie().getName();
                    if (agente.aceptaPropuesta(peliPropuesta)) { // SI EL AGENTE ACEPTA LA PROPUESTA TERMINA LA NEGOCIACION
                        respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta: " + peliPropuesta + " - ACEPTADA");
                        myAgent.send(respuesta);
                        myAgent.doDelete();
                    } else { // EL AGENTE RECHAZA LA PROPUESTA
                        this.getDataStore().put("propuestaDelOtroAgente", peliPropuesta); //guarda la propuesta actual del otro agente (q viene en el msj)
                        respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        respuesta.setContent(agente.getPropuestaActual());//cuando rechaza la propuesta q recibe, recuerda la suya
                        evaluarYResponder = true;
                        myAgent.send(respuesta);
                        System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta: " + peliPropuesta + " - RECHAZADA");
                    }
                } catch (UnreadableException ex) {
                    System.out.println("Error leyendo el contenido del msj");
                }
            } else {
                System.out.println("PROPUESTA NULL !!!");
                block();
            }
        }

        @Override
        public boolean done() {
            return evaluarYResponder;
        }
    }
}
