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
                "SELECT ISTREAM data, spolka, kursOtwarcia - min(kursOtwarcia) AS roznica FROM KursAkcji(spolka='Oracle').win:length(2) HAVING kursOtwarcia - min(kursOtwarcia) > 0");

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

//ZAD24
Polecenie: "SELECT IRSTREAM spolka AS X, kursOtwarcia AS Y FROM KursAkcji.win:length(3) WHERE spolka = 'Oracle'"
Odpowiedź: Klauzula WHERE nie ogranicza zdarzeń, które są wykorzystywane do stworzenia okna. Klauzula WHERE aplikowana jest dopiero przy filtrowaniu zdarzeń w strumieniu zdarzeń wstawianych i usuwanych

//ZAD25
Polecenie: "SELECT IRSTREAM data, kursOtwarcia, spolka FROM KursAkcji.win:length(3) WHERE spolka = 'Oracle'"

//ZAD26
Polecenie: "SELECT IRSTREAM data, kursOtwarcia, spolka FROM KursAkcji(spolka='Oracle').win:length(3)"

//ZAD27
Polecenie: "SELECT ISTREAM data, kursOtwarcia, spolka FROM KursAkcji(spolka='Oracle').win:length(3)"

//ZAD28
Polecenie: "SELECT ISTREAM data, max(kursOtwarcia), spolka FROM KursAkcji(spolka='Oracle').win:length(5)"

//ZAD29
Polecenie: "SELECT ISTREAM data, spolka, kursOtwarcia - max(kursOtwarcia) AS roznica FROM KursAkcji(spolka='Oracle').win:length(5)"
Odpowiedź: Funkcja max oblicza maksimum na podstawie wartości aktualnie znajdujących się w oknie. W SQL funkcja max oblicza maksimum na podstawie wartości znajdujących się w danej grupie (jeśli zastosowano grupowanie) lub na podstawie wartości znajdujących się w danej kolumnie (jeśli nie zastosowano grupowania)

//ZAD30
Polecenie: "SELECT ISTREAM data, spolka, kursOtwarcia - min(kursOtwarcia) AS roznica FROM KursAkcji(spolka='Oracle').win:length(2) HAVING kursOtwarcia - min(kursOtwarcia) > 0"
Odpowiedź: Wynik przetwarzania jest poprawny - uzyskane zostały tylko wzrosty

 */
