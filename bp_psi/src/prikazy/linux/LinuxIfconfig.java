/*
 * Předěláno ve čtvrtek 15.4.2010
 *
 * Materiály:
 *      http://www.benak.net/pocitace/os/linux/ifconfig.php
 *      http://books.google.cz/books?id=1x-6XBk8bKoC&pg=PT26&lpg=PT26&dq=ifconfig&source=bl&ots=E5ys4iqBVw&sig=0LU94iuXjoBE3WDxYGnTHChcx9Q&hl=cs&ei=O1lGS6CFHoOCnQOZzP3vAg&sa=X&oi=book_result&ct=result&resnum=8&ved=0CBkQ6AEwBw#v=onepage&q=ifconfig&f=false
 *      http://www.starhill.org/man/8_ifconfig.html
 * Dodělat:
 *      Snad už jenom parsujPrikaz() opravit.
 *      Ještě by chtělo ověřit divný masky typu: 1.1.1.1/32, 1.1.1.1/64 atp...
 * Odchylky:
 *      ifconfig eth0 2.2.2.2 3.3.3.3 fdsfsdfds 4.4.4.4 ve skutečnosti nastaví 3.3.3.3, u mě 4.4.4.4
 *              - opraveno 23.5.2010
 *      ifconfig eth0 netmask 255.0.0.0 2.2.2.2/2 ve skutečnosti spadne kvuli ty /2 za adresou. U mě to projde.
 *      ifconfig eth0 blabla netmask 255.255.255.0 ve skutečnosti žádnou masku nenastaví, protože přestane
 *          parsovat už u blabla.
 *      ifconfig eth0 netmask 255.255.255.0 10.0.0.1 by měl nastavit masku na 255.0.0.0, u mě na 255.255.255.0
 */
package prikazy.linux;

import prikazy.*;
import datoveStruktury.IpAdresa;
import java.util.ArrayList;
import java.util.List;
import pocitac.*;
import vyjimky.SpatnaMaskaException;

/**
 *
 * @author neiss
 */
public class LinuxIfconfig extends AbstraktniPrikaz {

    boolean ladiciVypisovani=false; //jestli se maj vypisovat informace pro ladeni
    boolean ladeni=false;

    String jmenoRozhrani; //jmeno rozhrani, jak bylo zadano
    /**
     * Do tyhle promenny se uklada jenom IP adresa bez masky za lomitkem.
     */
    List <String> seznamIP=new ArrayList<String>();
    String spatnaAdresa=null; //prvni ze spatnejch zadanejch adres - ta se totiz vypisuje
    /**
     * Maska jako String (napr. 255.255.255.0);
     */
    String maska;
    String broadcast;
    int pocetBituMasky = -1; //m zadana formou /24 totiz ma vetsi prioritu nez 255.255.255.0
    List <String> add=new ArrayList<String>(); //IP adresy, ktera se ma pridat
    List <String> del=new ArrayList<String>();  //ipadresy, ktery se maj odebrat
    List <String> neplatnyAdd=new ArrayList<String>(); //spatny IP adresy, ktera se meli pridat
    List <String> neplatnyDel=new ArrayList<String>();  //spatny ipadresy, ktery se meli odebrat
    boolean minus_a = false;
    boolean minus_v = false;
    boolean minus_s = false;
    boolean minus_h = false;
    
    /**
     * Do tyhle promenny bude metoda parsujPrikaz zapisovat, jakou chybu nasla:<br />
     * 0: vsechno v poradku<br />
     * 1: spatny prepinac (neznama volba)<br />
     * 2: nejaka chyba v gramatice prikazu (napr: ifconfig wlan0 1.2.3.5 netmask)
     *    potreba provest, co je dobre, a vypsat napovedu --help<br />
     * 4: rozhrani neexistuje<br />
     * 8: zadano vice ipadres, bere se posledni spravna<br />
     * 16: spatna  IP adresa<br />
     * 32: pocet bitu masky vetsi nez 32<br />
     * 64: neplatna IP adresa parametru add<br />
     * 128: neplatna IP adresa parametru del<br />
     * 256: zakazana IP adresa<br />
     */
    int navratovyKod = 0;
    SitoveRozhrani rozhrani; //rozhrani, se kterym se bude operovat
    int pouzitIp = -1; //cislo seznamIP, ktera IP se ma pouzit


    public LinuxIfconfig(AbstraktniPocitac pc, Konsole kon, List<String> slova) {
        super(pc, kon, slova);
        parsujPrikaz();
        zkontrolujPrikaz();
        vypisChybovyHlaseni();
        vykonejPrikaz();
    }

    protected void parsujPrikaz() {
        String tempRet;
        int ind = 1; //index v seznamu, zacina se jedicko, protoze prvnim slovem je ifconfig
        // prepinace:
        while (ind < slova.size() && slova.get(ind).indexOf("-") == 0) { //kdyz je prvnim znakem slova minus
            if (slova.get(ind).equals("-a")) {
                minus_a = true;
            } else if (slova.get(ind).equals("-v")) {
                minus_v = true;
            } else if (slova.get(ind).equals("-s")) {
                minus_s = true;
            } else if (slova.get(ind).equals("-h")||slova.get(ind).equals("--help")) {
                minus_h = true;
            } else {
                errNeznamyPrepinac(slova.get(ind));
                return; //tady ifconfig uz zbytek neprovadi, i kdyby byl dobrej
            }
            ind++;
        }
        //jmenoRozhrani je to prvni za prepinacema:
        if (ind >= slova.size()) {
            return;
        }
        jmenoRozhrani = slova.get(ind);
        ind++;
        if (ind >= slova.size()) {
            return;
        }
        //parametry:
        //Zjistil jsem, ze neznamej parametr se povazuje za adresu nebo za adresu s maskou.
        try { // celej cyklus je v bloku ,protoze by se mohlo stat, ze za nazvem parametru uz nebude jeho hodnota
            while (ind < slova.size()) {
                tempRet = slova.get(ind);
                if (tempRet.equals("netmask")) {//m
                    ind++;
                    maska = slova.get(ind);
                } else if (tempRet.equals("broadcast")) {//adresa pro broadcast, ta si vubec dela uplne, co se ji zachce
                    ind++;
                    broadcast = slova.get(ind);
                } else if (tempRet.equals("add")) {//adresa pro broadcast, ta si vubec dela uplne, co se ji zachce
                    ind++;
                    add.add(slova.get(ind));
                } else if (tempRet.equals("del")) {//adresa pro broadcast, ta si vubec dela uplne, co se ji zachce
                    ind++;
                    del.add(slova.get(ind));
                } else { //kdyz to neni nic jinyho, tak to ifconfig povazuje za seznamIP adresu
                    int pos = tempRet.indexOf('/');
                    if (pos != -1) { //zadano i s maskou za lomitkem
                        
                        try {
                            int temp = Integer.parseInt(tempRet.substring(pos + 1, tempRet.length()));
                            if(temp<0){
                                navratovyKod|=2;
                            }else{
                                seznamIP.add ( tempRet.substring(0, pos) ) ;
                                pocetBituMasky = temp;
                            }
                        } catch (NumberFormatException ex){
                            navratovyKod|=2;
                        }

                    } else {
                        seznamIP.add(tempRet);
                    }
                }
                ind++;
            }
        } catch (IndexOutOfBoundsException ex) { //tuhle vyjimku hazi radky s nactenim hodnoty parametru, kdyz
            navratovyKod |= 2;             //tam ta hodnota neni
        }
    }

    /**
     * Tahlecta metoda kontroluje jen hodnoty parametru, na nektery chyby, napr. gramaticky (nespravny
     * prepinace, vice parametru netmask ap.), predpokladam, ze uz se prislo. Posila klientovi hlaseni
     * o chybach.
     */
    private void zkontrolujPrikaz(){
        if (jmenoRozhrani==null) return; //uzivatel zadal jen ifconfig, mozna nejaky prepinace, ale nic vic
        //-------------------
        //kontrola existence rozhrani
        rozhrani=pc.najdiRozhrani(jmenoRozhrani);
        if (rozhrani==null){
            //tady se nic nevypisuje, protoze ostatni se v ifconfigu asi vyhodnocuje driv (kdyz je spatne
            //rozhrani i ipadresa, tak se jako spatna ukaze IP adresa
            navratovyKod |= 4;
        }
        //------------------------
        //kontrola IP
        if(seznamIP.size()>1){ //jestli neni moc IP adres
            navratovyKod |= 8;
        }
        for (int i=0;i<seznamIP.size();i++){ //kontrola spravnosti IP
            if ( ! IpAdresa.spravnaAdresaNebMaska(seznamIP.get(i), false) ) { //adresa neni spravna
                if(spatnaAdresa==null){
                    spatnaAdresa=seznamIP.get(i); //prvni ze spatnejch IP adres se uklada pro vypsani hlaseni
                }
                navratovyKod |= 16; //neplatna IP
            } else if(IpAdresa.jeZakazanaIpAdresa(seznamIP.get(i))){ //adresa je spravna, ale zakazana
                
                navratovyKod |= 256; //neplatna IP
            } else { //spravna adresa
                if((navratovyKod&16) == 0){ //po spatnz adrese se dalsi uz neberou
                    pouzitIp=i;
                }
            }

        }
        //--------------------
        //kontrola masky
        //string masky se nekontroluje, protoze pro to IpAdresa nema metodu, kontroluje se az pri nastavovani
        if(pocetBituMasky!=-1){ //kontrola pocetBituMasky
            if(pocetBituMasky>32){//mensi totiz bejt nemuze, to se kontroluje driv
                navratovyKod |= 32;
                pocetBituMasky=pocetBituMasky % 32; //takhle se ifconfig opravdu chova, vyzkousel jsem to
            }
        }
        //---------------------
        //kontrola IP adres add (pridavani nove IP)
        for(int i=0;i<add.size();i++){
            if( !IpAdresa.spravnaAdresaNebMaska(add.get(i), false) || IpAdresa.jeZakazanaIpAdresa(add.get(i)) ){
                navratovyKod |= 64;
                neplatnyAdd.add(add.get(i));
            }
        }
        for(int i=0;i<neplatnyAdd.size();i++){ //musi se to mazat v jinym cyklu, aby to nevylezlo ven
            add.remove(neplatnyAdd.get(i));
        }
        //---------------------
        //kontrola IP adres del (odebirani existujici IP)
        for(int i=0;i<del.size();i++){
            if ( !IpAdresa.spravnaAdresaNebMaska(del.get(i), false) || IpAdresa.jeZakazanaIpAdresa(del.get(i)) ){
                navratovyKod |= 128;

                neplatnyDel.add(del.get(i));
            }
        }
        for(int i=0;i<neplatnyDel.size();i++){ //musi se to mazat v jinym cyklu, aby to nevylezlo ven
            del.remove(neplatnyDel.get(i));
        }
        //---------------------
    }

    /**
     * Metoda na vypisovani chybovejch hlášení. Projde návratovej kód a pošle
     * hlášení podle jejich priority.
     * O prioritách více v sešitě (14.4.) a v souboru IfconfigChyby.txt.
     * Odpovídá  metodě vykonejPrikaz() ve starý versi Ifconfigu.
     */
    private void vypisChybovyHlaseni(){
        if(ladeni){
            kon.posliRadek(toString());
            kon.posliRadek("----------------------------------");
        }

        // Serazeny je to podle priority - co se vypise driv:
        if (navratovyKod == 0) { // v poradku
            //nic se nevypisuje
        }
        if ((navratovyKod & 1) != 0) {
            //Spatnej prepinac, to se nic neprovadi
            //Jediny hlaseni, ktery se vypisuje uz driv v parseru, ma stejne nejvyssi prioritu a
            //nic dalsiho uz se neprovadi, tak by to byla akorat zbytecna prace
            return; //nic dalsiho se neprovadi
        }
        if ((navratovyKod & 16) != 0) { //aspon jedna z adres je neplatna
            kon.posliRadek(spatnaAdresa+": unknown host");
            kon.posliRadek("ifconfig: `--help' gives usage information.");
            return;
        }
        if ((navratovyKod & 4) != 0) { //rozhrani neexistuje
            if(pouzitIp!=-1) //adresa byla zadana
                kon.posliRadek("SIOCSIFADDR: No such device");
            kon.posliRadek(jmenoRozhrani + ": error fetching interface information: Device not found");
            // vypis o masce ma mensi prioritu, je az pod chybou v gramatice (navratovyKod & 2)
        }
        if ((navratovyKod & 256) != 0) {//zakazana ip adresa
            if((navratovyKod & 4) == 0) //vypisuje se, jen kdyz rozhrani je v poradku
                kon.posliRadek("SIOCSIFADDR: Invalid argument");
        }
        if ((navratovyKod & 64) != 0) { //neplatna adresa add
            for(int i=0;i<neplatnyAdd.size();i++){ //vsechny se poporade vypisou
                kon.posliRadek(neplatnyAdd.get(i)+": unknown host");
            }
        }
        if ((navratovyKod & 128) != 0) { //vsechny se poporade vypisou
            for(int i=0;i<neplatnyDel.size();i++){
                kon.posliRadek(neplatnyDel.get(i)+": unknown host");
            }
        }
        if ((navratovyKod & 4) != 0) { //rozhrani neexistuje
            //pokracovani zezhora - vypis o masce ma totiz nizsi prioritu
            if(pocetBituMasky!=-1 ||maska!=null) //maska byla zadana
                kon.posliRadek("SIOCSIFNETMASK: No such device");
        }
        if ((navratovyKod & 2) != 0) { //nejaka chyba v gramatice
            vypisHelp();
            if (ladiciVypisovani) {
                kon.posliRadek("blok pro navratovy kod 2, navratovy kod:" + navratovyKod);
            }
        }
        

        
        if ((navratovyKod & 8) != 0) { //zadano vice ip adres
            //nic se nevypisuje
        }
        if ((navratovyKod & 32) != 0) {//pocetBituMasky byl vetsi nez 32,
            // metoda zkontrolujPrikaz to uz opravila
            // nic se nevypisuje
        }

        if(broadcast!=null || add.size()>0 ||del.size()>0){
            kon.posliServisne("Parametry broadcast, add a del prikazu ifconfig zatim nejsou podporovane.");
        }
        
        
    }

    /**
     * Vykonava samotnej prikaz.
     * U navratovyhoKodu 1 (spatnejPrepinac) a 4 (rozhrani neexistuje) nic neprovadi.
     * Odpovidá metodě proved() ze starý verse ifconfigu.
     */
    @Override
    protected void vykonejPrikaz() {
        if(minus_h){
            vypisHelp();
            return;
        }
        if ((navratovyKod & 4) != 0 || ((navratovyKod) & 1) != 0) { //kdyz navratovy kod obsahuje 4 nebo 1
            // U navratovyhoKodu 1 (spatnejPrepinac) a 4 (rozhrani neexistuje) nic neprovadi.
        } else {
            if (rozhrani == null) { //vypsat vsechno
                if (navratovyKod == 0) { //vypisuje se, jen kdyz je to ale vsechno v poradku
                    for (SitoveRozhrani rozhr : pc.rozhrani) {
                        vypisRozhrani(rozhr);
                    }
                }
            } else { //rozhrani bylo zadano
                if (seznamIP.size() == 0 && add.size() == 0 && del.size() == 0
                        && maska == null && broadcast == null) { //jenom vypis rozhrani
                    if (navratovyKod == 0) { //vypisuje se, jen kdyz je to ale vsechno v poradku
                        vypisRozhrani(rozhrani);
                    }
                } else { //nastavovani
                    nastavAdresuAMasku(rozhrani);
                    //nastavovani broadcastu zatim nepodporuju
                    //nastavovani parametru add zatim nepodporuju
                    //nastavovani parametru del zatim nepodporuju
                }
            }
        }
    }

    /**
     * Pokusi se nejprve nastavit adresu (pokud je zadana), pak masku ze Stringu (je-li zadana)
     * a nakonec i masku z pocetBituMasky (je-li zadana), protoze ta ma vetsi prioritu. Pokud se
     * provedla nejaka zmena vyridi nakonec routovaci tabulku. Sama nic nenastavuje, ale pouziva
     * k tomu privatni metody. Je-li zadana maska obema zpusoby, zmeni se dvakrat (tim padem i
     * routovaci tabulka, i kdyz je vysledek stejnej jako predchozi hodnoty, napr:
     * ifconfig eth0 1.1.1.1/24 netmask 255.255.0.0 se zmeni nejprv na tu ze stringu, pak na
     * tu za lomitkem)
     * @param r
     */
    private void nastavAdresuAMasku(SitoveRozhrani r) { //nastavuje ip
        boolean zmena=false; // jestli se vykonala nejaka zmena, nebo jestli zadany hodnoty byly stejny 
                                // jako puvodni -> kvuli zmenam routovaci tabulky

        //nastavovani adresy:
        if (pouzitIp != -1){ //adresa byla zadana, musi se nastavit
            String nastavit = seznamIP.get(pouzitIp);
            if (r.vratPrvni()!=null && nastavit.equals(r.vratPrvni().vypisAdresu())) {
                //ip existuje a je stejna, nic se nemeni
            } else { //IP adresa neni stejna, bude se menit
                r.zmenPrvniAdresu(vytvorAdresu(nastavit));
                zmena=true;
            }
        }

        //nastavovani masky ze Stringu m
        if (maska != null) { //zadana adresa s maskou za lomitkem
            if(r.vratPrvni()!=null && r.vratPrvni().vypisMasku().equals(maska)){
                //ip adresa existuje a ma stejnou masku, nic se nemeni
            }else{//zadana hodnota je jina nez puvodni, musi se menit
                priradMasku(r.vratPrvni(), maska);
                zmena=true;
            }
        }

        //nastavovani masky za lomitkem
        if (pocetBituMasky != -1) { //zadana adresa s maskou za lomitkem
            if(r.vratPrvni()!=null && r.vratPrvni().pocetBituMasky()==pocetBituMasky){
                //ip adresa existuje a ma stejnou masku, nic se nemeni
            }else{//zadana hodnota je jina nez puvodni, musi se menit
                priradMasku(r.vratPrvni(), pocetBituMasky);
                zmena=true;
            }
        }

        //kdyz se provedla nejaka zmena, musi se to projevit v routovaci tabulce:
        if(zmena)vyridRoutovani(r);

    }

    /**
     * Vytvori novou adresu, nenastavuje masku, ale hlida, jestli IpAdresu lze pouzit,
     * nebo jestli na ni neni nejaka specialni akce.
     * @param ip
     * @return null pro 0.0.0.0
     */
    private IpAdresa vytvorAdresu(String adr){
        if(adr.equals("0.0.0.0")){ //mazani adresy z rozhrani
            return null;
        }else{
            return new IpAdresa(adr);
        }
    }

    /**
     * Zadane IP adrese nastavi masku podle zadaneho poctuBitu (masky). pocetBitu musi bejt spravny cislo.
     * Kdyz je adresa null, posle chybovy hlaseni a skonci.
     * @param ip
     * @param pocetBitu
     */
    private void priradMasku(IpAdresa ip, int pocetBitu){
        if(ip==null){
            kon.posliRadek("SIOCSIFNETMASK: Cannot assign requested address");
            return;
        }else{
            ip.nastavMasku(pocetBitu);
        }
    }

    /**
     * Pokusi se nastavit masku podle parametru m, ktery musi bejt spravnym stringem.
     * Je-li zadana IP null, vypise chybovy hlaseni a ukonci se.
     * @param ip adresa, ktera se ma zmenit
     * @param m string masky; nesmi bejt null
     */
    private void priradMasku(IpAdresa ip, String m){//pokusi se nastavit masku
        if(ip==null){ //neni nastavena IP adresa, vypise se chybovy hlaseni a skonci se
            kon.posliRadek("SIOCSIFNETMASK: Cannot assign requested address");
            return;
        }
        try{//je potreba zkontrolovat spravnost masky!!! //proto vyjimka
            ip.nastavMasku(m);
        }catch(SpatnaMaskaException ex){
            kon.posliRadek("SIOCSIFNETMASK: Invalid argument");
            return;
        }
    }

    private void vyridRoutovani(SitoveRozhrani r){
        pc.routovaciTabulka.smazVsechnyZaznamyNaRozhrani(r); //mazani rout
        if(r.vratPrvni()!=null){
            pc.routovaciTabulka.pridejZaznam(r.vratPrvni().vratCisloSite(), r);
        }
    }


    private void vypisRozhrani(SitoveRozhrani r){
        int a = (int) (Math.random() * 100); //nahodne cislo 0 - 99
        int b = (int) (Math.random() * 100); //nahodne cislo 0 - 99

        kon.posliRadek(r.jmeno + "\tLink encap:Ethernet  HWadr " + r.macAdresa);
        if (r.vratPrvni() != null) {
            kon.posliRadek("\tinet adr:" + r.vratPrvni().vypisAdresu() + "  Bcast:"
                    + r.vratPrvni().vypisBroadcast() +
                    "  Mask:" + r.vratPrvni().vypisMasku());
        }
        kon.posliRadek("\tUP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1"); //asi ne cesky
        if (r.pripojenoK != null) {
            kon.posliRadek("\tRX packets:" + (a * b) + " errors:" + (b / 50) + "+ dropped:"
                    + (a / 20) + " overruns:" + (a / 50) + " frame:0");
            kon.posliRadek("\tTX packets:" + (b * 100 + a) + " errors:0 dropped:0 overruns:0 carrier:0");
        } else {
            kon.posliRadek("\tRX packets:0 errors:0 dropped:0 overruns:0 frame:0");
            kon.posliRadek("\tTX packets:0 errors:0 dropped:0 overruns:0 carrier:0");
        }
        kon.posliRadek("\tcollisions:0 txqueuelen:1000");
        if (r.pripojenoK != null) {
            kon.posliRadek("\tRX bytes:" + (a * 1000 + b * 10 + (a / 10)) + " ("
                    + ((a * 1000 + b * 10 + (a / 10)) / 1000) + " KiB)  TX bytes:1394 (1.3 KiB)");
        } else {
            kon.posliRadek("\tRX bytes:0 (0 KiB)  TX bytes:1394 (1.3 KiB)");
        }
        kon.posliRadek("\tInterrupt:12 Base address:0xdc00");
        kon.posliRadek("");
    }

    @Deprecated //zjistil jsem, ze tahle metoda vlastne neni vubec potreba
    private void unknownHost(String vypsat){
        kon.posliRadek(vypsat+": Unknown host");
        kon.posliRadek("ifconfig: `--help' vypíše návod k použití.)");
    }

    @Override
    public String toString() {
        String vratit = "  Parametry prikazy ifconfig:\n\r\tnavratovyKodParseru: " 
                + rozlozNaMocniny2(navratovyKod);
        if (jmenoRozhrani != null) {
            vratit += "\n\r\trozhrani: " + jmenoRozhrani;
        }
        if (seznamIP != null) {
            vratit += "\n\r\tip: " + seznamIP;
        }
        vratit+="\n\r\tpouzitIp: "+pouzitIp;
        if (pocetBituMasky != -1) {
            vratit += "\n\r\tpocetBituMasky: " + pocetBituMasky;
        }
        if (maska != null) {
            vratit += "\n\r\tmaska: " + maska;
        }
        if (add != null) {
            vratit += "\n\r\tadd: " + add;
        }
        if (del != null) {
            vratit += "\n\r\tdel: " + del;
        }

        return vratit;

    }

    private void errNeznamyPrepinac(String ret) {
        kon.posliRadek("ifconfig: neznámá volba `" + ret + "'.");
        kon.posliRadek("ifconfig: `--help' vypíše návod k použití.");
        navratovyKod = 1;
    }

    private void vypisHelp() { // funkce na ladiciVypisovani napovedy --help
        kon.posliRadek("Usage:");
        kon.posliRadek("  ifconfig [-a] [-v] [-s] <interface> [[<AF>] <address>]");
        kon.posliRadek("  [add <address>[/<prefixlen>]]");
        kon.posliRadek("  [del <address>[/<prefixlen>]]");
        kon.posliRadek("  [[-]broadcast [<address>]]  [[-]pointopoint [<address>]]");
        kon.posliRadek("  [netmask <address>]  [dstaddr <address>]  [tunnel <address>]");
        kon.posliRadek("  [outfill <NN>] [keepalive <NN>]");
        kon.posliRadek("  [hw <HW> <address>]  [metric <NN>]  [mtu <NN>]");
        kon.posliRadek("  [[-]trailers]  [[-]arp]  [[-]allmulti]");
        kon.posliRadek("  [multicast]  [[-]promisc]");
        kon.posliRadek("  [mem_start <NN>]  [io_addr <NN>]  [irq <NN>]  [media <type>]");
        kon.posliRadek("  [txqueuelen <NN>]");
        kon.posliRadek("  [[-]dynamic]");
        kon.posliRadek("  [up|down] ...");
        kon.posliRadek("");
        kon.posliRadek("  <HW>=Hardware Type.");
        kon.posliRadek("  List of possible hardware types:");
        kon.posliRadek("    loop (Local Loopback) slip (Serial Line IP) cslip (VJ Serial Line IP)");
        kon.posliRadek("    slip6 (6-bit Serial Line IP) cslip6 (VJ 6-bit Serial Line IP) adaptive (Adaptive Serial Line IP)");
        kon.posliRadek("    strip (Metricom Starmode IP) ash (Ash) ether (Ethernet)");
        kon.posliRadek("    tr (16/4 Mbps Token Ring) tr (16/4 Mbps Token Ring (New)) ax25 (AMPR AX.25)");
        kon.posliRadek("    netrom (AMPR NET/ROM) rose (AMPR ROSE) tunnel (IPIP Tunnel)");
        kon.posliRadek("    ppp (Point-to-Point Protocol) hdlc ((Cisco)-HDLC) lapb (LAPB)");
        kon.posliRadek("    arcnet (ARCnet) dlci (Frame Relay DLCI) frad (Frame Relay Access Device)");
        kon.posliRadek("    sit (IPv6-in-IPv4) fddi (Fiber Distributed Data Interface) hippi (HIPPI)");
        kon.posliRadek("    irda (IrLAP) ec (Econet) x25 (generic X.25)");
        kon.posliRadek("    eui64 (Generic EUI-64)");
        kon.posliRadek("  <AF>=Address family. Default: inet");
        kon.posliRadek("  List of possible address families:");
        kon.posliRadek("    unix (UNIX Domain) inet (DARPA Internet) inet6 (IPv6)");
        kon.posliRadek("    ax25 (AMPR AX.25) netrom (AMPR NET/ROM) rose (AMPR ROSE)");
        kon.posliRadek("    ipx (Novell IPX) ddp (Appletalk DDP) ec (Econet)");
        kon.posliRadek("    ash (Ash) x25 (CCITT X.25)");
    }
    
}
