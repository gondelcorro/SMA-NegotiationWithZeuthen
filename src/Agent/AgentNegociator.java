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
import jade.util.leap.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import util.Utilidades;

public class AgentNegociator extends Agent {

    private AID AgentReceiverID;
    //private HashMap<Movie, Float> peliculasVotadas; //se supone vistas
    //private HashMap<Movie, Float> utilidades;
    private ArrayList<Movie> catalogoPeliculas; //todas las pelis 
    private ArrayList<Utilidades> listaUtilidades;
    //private ArrayList<Movie> propuestasDisponibles; // no vistas
    //private ArrayList<Movie> propDispOrdenadas; // de mayor a manor segun la utilidad
    private Boolean inicio = false;
    private Movie propuestaActual;
    private float utilidadActual;

    protected void setup() {
        System.out.println("*** Bienvenido agente " + this.getLocalName() + " ***");
        System.out.println("Sus utilidades son:");
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
            //this.loadPeliculasVotadas();
            //this.loadPropuestasDisp();
            this.loadUtilidades();
            //this.ordenarPropuestasDisponibles();
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

    /*
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
     */
    private void loadUtilidades() {// utilidades tendra un rating asignado para c/pelicula del catalogo
        //utilidades = new HashMap<>(); //<Moviee, Float>
        listaUtilidades = new ArrayList<>();
        for (Movie peli : catalogoPeliculas) {
            Utilidades utilidad = new Utilidades();
            utilidad.setMovie(peli);
            if (this.getLocalName().equals("A")) {
                float util = (float) Math.abs(Math.random() * 1);
                int aux = (int) (util * 1000);
                float result = aux / 1000f;
                utilidad.setUtilidad(result);
            } else {
                float util = (float) Math.abs(Math.random() * 5);
                int aux = (int) (util * 100);//1243
                float result = aux / 100f;//12.43
                utilidad.setUtilidad(result);
            }
            listaUtilidades.add(utilidad);
        }
        Collections.sort(listaUtilidades); // el sorte ordena de manera ascendente
        Collections.reverse(listaUtilidades); // con reverse invierto el orden (descendente)
        listaUtilidades.forEach(item -> System.out.print(item.getMovie().getName() + " - " + item.getUtilidad() + " | "));
    }

    /*
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
     */
    public Movie elegirPropuesta() { // La eleccion es tomar la 1era pelicula de pelisDisponibles
        if (!this.listaUtilidades.isEmpty()) {
            Utilidades utilidad = this.listaUtilidades.remove(0);
            propuestaActual = utilidad.getMovie();
            utilidadActual = utilidad.getUtilidad();
            return propuestaActual;
        }
        return null;
    }

    public boolean aceptaPropuesta(Movie peli) {// se acepta si la utilidad d la peli prop es > q la prop actual
        
        float utilidadPeliPropuesta = getUtilidad(peli);
        //float utilidadMiPropuesta = getUtilidad(this.getPropuestaActual());
        System.out.println("Agente " + this.getLocalName() + ": UPeliPropuesta(" + peli.getName() + ")= " +utilidadPeliPropuesta + " | UMiPropuesta(" + propuestaActual.getName() + ")=" + this.utilidadActual );
        return (utilidadPeliPropuesta >= this.utilidadActual); //retorna true (acepta) si la utilidad de peliPropuesta es mayor o igual a la utilidad de mi propuesta
    }

    public float getUtilidad(Movie movie) {

        float utilidad = 0;
        for (Utilidades u : listaUtilidades) {
            if (u.getMovie().equals(movie)) {
                utilidad = u.getUtilidad();
                break;
            }
        }
        return utilidad;
    }
    
    public float getUtilidadActual(){
        return this.utilidadActual;
    }

    public Movie getPropuestaActual() {
        return this.propuestaActual;
    }
}
