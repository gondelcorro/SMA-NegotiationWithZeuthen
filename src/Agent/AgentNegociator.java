package Agent;

import Behaviour.FSM_Negotiation;
import Concepts.Movie;
import Ontology.MCPOntology;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AgentNegociator extends Agent {

    private AID AgentReceiverID;
    private HashMap<Movie, Float> peliculasVotadas; //se supone vistas
    private HashMap<Movie, Float> utilidades;
    private ArrayList<Movie> catalogoPeliculas; //todas las pelis 
    private ArrayList<Movie> propuestasDisponibles; // no vistas
    private ArrayList<Movie> propDispOrdenadas; // de mayor a manor segun la utilidad
    private Boolean inicio = false;
    private Movie propuestaActual;

    protected void setup() {
        System.out.println("*** Bienvenido agente " + this.getLocalName() + " ***");
        try {
            getContentManager().registerLanguage(MCPOntology.getCodecInstance());
            getContentManager().registerOntology(MCPOntology.getInstance());
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("negociacion");
            sd.setName("peliculas");
            dfd.addServices(sd);

            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(new Long(1));

            this.loadPeliculas();
            this.loadPeliculasVotadas();
            this.loadPropuestasDisp();
            this.loadUtilidades();
            this.ordenarPropuestasDisponibles();
            this.propuestaActual = this.elegirPropuesta(); //elegir la propuesta inicial segun MCP!

            DFAgentDescription[] result = DFService.search(this, dfd);
            if (result.length > 0) {
                System.out.println("HAY SERVICIO - ME SUSCRIBO");
                this.addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), dfd, sc)) {
                    @Override
                    protected void handleInform(ACLMessage inform) {
                        try {
                            DFAgentDescription[] result = DFService.decodeNotification(inform.getContent());
                            if (result.length > 0) {
                                AgentReceiverID = result[0].getName();
                                addBehaviour(new FSM_Negotiation(AgentReceiverID, inicio));
                            }
                        } catch (FIPAException ex) {
                            System.out.println("error en la suscripcion al servicio: " + ex);
                        }
                    }
                });
            } else {
                System.out.println("NO HAY SERVICIO - PUBLICO");
                DFService.register(this, dfd);
                inicio = true;
                this.addBehaviour(new FSM_Negotiation(this.getAID(), inicio));
            }
        } catch (FIPAException ex) {
            System.out.println("error buscando el servicio: " + ex);
        }
    }

    @Override
    protected void takeDown() {
        /* if (!suscripcionCancelada) {
            try {
                DFService.deregister(this);
                suscripcionCancelada = true;
                System.out.println("Agente " + getAID().getName() + " cancela su servicio.");
            } catch (FIPAException ex) {
                System.out.println("FIPA EXCEPTION: " + ex);
            }
        }*/
        System.out.println("Agente " + getAID().getLocalName() + " terminado.");
    }

    private void loadPeliculas() {
        catalogoPeliculas = new ArrayList<>();
        File archivo = null;
        FileReader fr = null;
        BufferedReader br = null;
        try {
            archivo = new File("CatalogoPeliculas.txt");
            fr = new FileReader(archivo);
            br = new BufferedReader(fr);
            String linea;
            while ((linea = br.readLine()) != null) {
                Movie movie = new Movie();
                movie.setName(linea);
                catalogoPeliculas.add(movie);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fr) {
                    fr.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private void loadPeliculasVotadas() {//elijo una peli aleatoriamente del cataloPeliculas y le calculo un rating aleatorio
        peliculasVotadas = new HashMap<>(); //<Moviee, Float>
        ArrayList listaRandomPeliPos = new ArrayList();
        for (int i = 0; i < 10; i++) { //se votan 10 pelis aleatoriamente
            int randomPeliPos = (int) Math.abs(Math.random() * catalogoPeliculas.size());
            if (!listaRandomPeliPos.contains(randomPeliPos)) {
                listaRandomPeliPos.add(randomPeliPos);
                if (this.getLocalName().equals("A")) {
                    peliculasVotadas.put(catalogoPeliculas.get(randomPeliPos), (float) Math.abs(Math.random() * 1));
                } else {
                    peliculasVotadas.put(catalogoPeliculas.get(randomPeliPos), (float) Math.abs(Math.random() * 5));
                }
            }
        }
        System.out.println("Sus 10 pelis votadas son:");
        peliculasVotadas.forEach((k, v) -> System.out.print("Peli: " + k.getName() + " Rating: " + v + " - "));
    }

    private void loadPropuestasDisp() {// las pelis disponibles son las q no voto (se asume q no las vio)
        this.propuestasDisponibles = new ArrayList<>();
        for (Movie peli : catalogoPeliculas) {
            if (!peliculasVotadas.containsKey(peli)) {
                propuestasDisponibles.add(peli);
            }
        }
        Collections.shuffle(propuestasDisponibles);//Randomiza la lista
        System.out.println("\nPelis disponibles: " + propuestasDisponibles);
    }

    private void loadUtilidades() {// utilidades tendra un rating asignado para c/pelicula del catalogo
        utilidades = new HashMap<>(); //<Moviee, Float>
        for (Movie peli : catalogoPeliculas) {
            if (peliculasVotadas.containsKey(peli)) {
                utilidades.put(peli, peliculasVotadas.get(peli));// la peli y el rating q se le puso xq ya esta votada
            } else {
                utilidades.put(peli, (float) Math.abs(Math.random() * (5))); //si la peli no fue votada le genero aleatoriamente un rating
            }
        }
    }

    private void ordenarPropuestasDisponibles() {
        //Ordenar la lista de peliculas disponibles por mayor utilidad en el hashmap utilidades
        this.propDispOrdenadas = new ArrayList<>();
        HashMap<Movie, Float> utilidadesAux = (HashMap<Movie, Float>) utilidades.clone();
        for (Movie peli : propuestasDisponibles) {
            if (utilidadesAux.containsKey(peli)) {
                Movie peliConMayorUtilidad = (Movie) Collections.max(utilidadesAux.entrySet(), Map.Entry.comparingByValue()).getKey();
                if(peli.getName().equals(peliConMayorUtilidad.getName())){
                    propDispOrdenadas.add(peli);
                    utilidadesAux.remove(peli);
                }
            }
        }
    }

    public Movie elegirPropuesta() { // La eleccion es tomar la 1era pelicula de pelisDisponibles
        if (!this.propDispOrdenadas.isEmpty()) {
            propuestaActual = this.propDispOrdenadas.remove(0);
            return propuestaActual;
        }
        return null;
    }

    public boolean aceptaPropuesta(Movie peli) {// se acepta si la utilidad d la peli prop es > q la prop actual
        System.out.println("Agente " + this.getLocalName() + ": UPeliPropuesta(" + peli.getName() + ")= " + this.getUtilidad(peli) + " | UMiPropuesta(" + propuestaActual.getName() + ")=" + this.getUtilidad(this.propuestaActual));
        return (this.getUtilidad(peli) >= this.getUtilidad(this.getPropuestaActual()));
    }

    public float getUtilidad(Movie peli) {
        return utilidades.get(peli);
    }

    public Movie getPropuestaActual() {
        return this.propuestaActual;
    }
}
