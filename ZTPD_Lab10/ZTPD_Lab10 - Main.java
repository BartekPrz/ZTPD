import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(KursAkcji.class);
        EPRuntime epRuntime = EPRuntimeProvider.getDefaultRuntime(configuration);

        EPDeployment deployment = compileAndDeploy(epRuntime,
                "SELECT data, spolka, obrot FROM KursAkcji(market='NYSE').win:ext_timed_batch(data.getTime(), 7 days) order by obrot desc limit 1 offset 2");

        ProstyListener prostyListener = new ProstyListener();
        for (EPStatement statement : deployment.getStatements()) {
            statement.addListener(prostyListener);
        }

        InputStream inputStream = new InputStream();
        inputStream.generuj(epRuntime.getEventService());
    }

    public static EPDeployment compileAndDeploy(EPRuntime epRuntime, String epl) {
        EPDeploymentService deploymentService = epRuntime.getDeploymentService();
        CompilerArguments args = new CompilerArguments(epRuntime.getConfigurationDeepCopy());
        EPDeployment deployment;
        try {
            EPCompiled epCompiled = EPCompilerProvider.getCompiler().compile(epl, args);
            deployment = deploymentService.deploy(epCompiled);
        } catch (EPCompileException | EPDeployException e) {
            throw new RuntimeException(e);
        }
        return deployment;
    }
}

/*

//Zad4a
Zdarzenia RSTREAM zostały wygenerowane tak późno, ponieważ dopiero gdy do systemu trafiło zdarzenie z datą 17.09, to spowodowało one, że zdarzenia z datą wcześniejszą jak 11.09 musiały zostać usunięte z okna, gdyż nie mieściły się w zadanym limicie czasowym 7 dniu

//Zad4b
Pierwsza porcja batcha zawierała zdarzenia z okresu 05.09-11.09 i została zarejestrowana w oknie w momencie pojawienia się zdarzenia z datą 17.09. Druga porcja batcha zawierała zdarzenia z okresu 12.09-18.09 i została zarejestrowana w oknie w momencie pojawienia się zdarzenia z datą 19.09. Trzecia porcja batcha zawierała zdarzenia z okresu 19.09-25.09, ale niestety nie została zarejestrowana w oknie, ponieważ w systmie nie pojawiło się żadne zdarzenie z datą późniejszą jak 25.09, co tłumaczy brak reszty zdarzeń ISTREAM
        
Zdarzenia RSTREAM zostały zarejestrowane tylko dla pierwszej porcji batcha, ponieważ trzecia porcja batcha nie trafiła nawet do okna, a druga porcja cały czas się w tym oknie znajduje i będzie się znajdować do momentu, gdy pojawi się wspomniana trzecia porcja, co tłumaczy brak reszty zdarzeń RSTREAM

//Zad5
Polecenie: "SELECT ISTREAM data, kursZamkniecia, spolka, max(kursZamkniecia) - kursZamkniecia as roznica FROM KursAkcji.win:ext_timed_batch(data.getTime(), 1 day)"

//Zad6
Polecenie: "SELECT ISTREAM data, kursZamkniecia, spolka, max(kursZamkniecia) - kursZamkniecia as roznica FROM KursAkcji(spolka in ('IBM', 'Honda', 'Microsoft')).win:ext_timed_batch(data.getTime(), 1 day)"

//Zad7a
Polecenie: "SELECT ISTREAM data, kursZamkniecia, kursOtwarcia, spolka FROM KursAkcji(kursZamkniecia > kursOtwarcia).win:length(1)"

//Zad7b
Polecenie: "SELECT ISTREAM data, kursZamkniecia, kursOtwarcia, spolka FROM KursAkcji(KursAkcji.roznicaKursow(kursOtwarcia, kursZamkniecia) > 0).win:length(1)"

//Zad8
Polecenie: "SELECT ISTREAM data, kursZamkniecia, spolka, max(kursZamkniecia) - kursZamkniecia as roznica FROM KursAkcji(spolka in ('CocaCola', 'PepsiCo')).win:ext_timed(data.getTime(), 7 days)"

//Zad9
Polecenie: "SELECT ISTREAM data, kursZamkniecia, spolka, max(kursZamkniecia) - kursZamkniecia as roznica FROM KursAkcji(spolka in ('CocaCola', 'PepsiCo')).win:ext_timed_batch(data.getTime(), 1 day) HAVING max(kursZamkniecia) = kursZamkniecia"

//Zad10
Polecenie: "SELECT ISTREAM max(kursZamkniecia) as maksimum FROM KursAkcji.win:ext_timed_batch(data.getTime(), 7 days)"

//Zad11
Polecenie: "SELECT k.kursZamkniecia as kursCoc, p.data, p.kursZamkniecia as kursPep FROM KursAkcji(spolka='PepsiCo').win:length(1) as p full outer join KursAkcji(spolka='CocaCola').win:length(1) as k on p.data = k.data WHERE p.kursZamkniecia > k.kursZamkniecia"

//Zad12
Polecenie: "SELECT k.data, k.kursZamkniecia as kursBiezacy, k.spolka, k.kursZamkniecia - a.kursZamkniecia as roznica FROM KursAkcji(spolka in ('PepsiCo', 'CocaCola')).win:length(1) as k join KursAkcji(spolka in ('PepsiCo', 'CocaCola')).std:firstunique(spolka) as a on k.spolka = a.spolka"

//Zad13
Polecenie: "SELECT k.data, k.kursZamkniecia as kursBiezacy, k.spolka, k.kursZamkniecia - a.kursZamkniecia as roznica FROM KursAkcji.win:length(1) as k join KursAkcji.std:firstunique(spolka) as a on k.spolka = a.spolka WHERE k.kursZamkniecia > a.kursZamkniecia"

//Zad14
Polecenie: "SELECT k.data as dataB, a.data as dataA, k.spolka, a.kursOtwarcia as kursA, k.kursOtwarcia as kursB  FROM KursAkcji.win:ext_timed(data.getTime(), 7 days) as k join KursAkcji.win:ext_timed(data.getTime(), 7 days) as a on k.spolka = a.spolka WHERE k.kursOtwarcia - a.kursOtwarcia > 3"

//Zad15
Polecenie: "SELECT data, spolka, obrot FROM KursAkcji(market='NYSE').win:ext_timed_batch(data.getTime(), 7 days) order by obrot desc limit 3"

//Zad16
Polecenie: "SELECT data, spolka, obrot FROM KursAkcji(market='NYSE').win:ext_timed_batch(data.getTime(), 7 days) order by obrot desc limit 1 offset 2"

 */