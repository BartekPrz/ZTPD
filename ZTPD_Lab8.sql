--ZAD1
CREATE TABLE CYTATY AS
SELECT * FROM ZSBD_TOOLS.cytaty;

--ZAD2
SELECT AUTOR, TEKST
FROM CYTATY
WHERE UPPER(TEKST) LIKE '%PESYMISTA%' AND UPPER(TEKST) LIKE '%OPTYMISTA%';

--ZAD3
CREATE INDEX CYTATY_IDX ON CYTATY(TEKST) INDEXTYPE IS CTXSYS.CONTEXT;

--ZAD4
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'pesymista and optymista') > 0;

--ZAD5
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'pesymista ~ optymista') > 0;

--ZAD6
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'near((pesymista, optymista), 3)') > 0;

--ZAD7
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'near((pesymista, optymista), 10)') > 0;

--ZAD8
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'życi%') > 0;

--ZAD9
SELECT AUTOR, TEKST, SCORE(1)
FROM CYTATY
WHERE CONTAINS(TEKST, 'życi%', 1) > 0;

--ZAD10
SELECT AUTOR, TEKST, SCORE(1) AS DOPASOWANIE
FROM CYTATY
WHERE CONTAINS(TEKST, 'życi%', 1) > 0
ORDER BY DOPASOWANIE DESC
FETCH FIRST 1 ROW ONLY;

--ZAD11
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'fuzzy(probelm,,,N)') > 0;

--ZAD12
INSERT INTO CYTATY VALUES(39, 'Bertrand Russell', 'To smutne, że głupcy są tacy pewni siebie, a ludzie rozsądni tacy pełni wątpliwości');
COMMIT;

--ZAD13
SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'głupcy') > 0;

--Brak otrzymanych wyników w zapytaniu spowodowany jest brakiem informacji w indeksie(na podstawie którego odbywa się wyszukiwanie) na temat nowo dodanego wiersza - niezaktualizowany indeks

--ZAD14
SELECT *
FROM DR$CYTATY_IDX$I;

SELECT *
FROM DR$CYTATY_IDX$I
WHERE TOKEN_TEXT = 'GŁUPCY';

--ZAD15
DROP INDEX CYTATY_IDX;

CREATE INDEX CYTATY_IDX ON CYTATY(TEKST) INDEXTYPE IS CTXSYS.CONTEXT;

--ZAD16
SELECT *
FROM DR$CYTATY_IDX$I
WHERE TOKEN_TEXT = 'GŁUPCY';

SELECT AUTOR, TEKST
FROM CYTATY
WHERE CONTAINS(TEKST, 'głupcy') > 0;

--ZAD17
DROP INDEX CYTATY_IDX;
DROP TABLE CYTATY;



--Zaawansowane indeksowanie i wyszukiwanie
--ZAD1
CREATE TABLE QUOTES AS
SELECT * FROM ZSBD_TOOLS.quotes;

--ZAD2
CREATE INDEX QUOTES_IDX ON QUOTES(TEXT) INDEXTYPE IS CTXSYS.CONTEXT;

--ZAD3
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'work') > 0;

SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, '$work') > 0;

SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'working') > 0;


SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, '$working') > 0;

--ZAD4
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'it') > 0;

--System nie zwrócił wyników, ponieważ słówko it w języku angielskim jest słowem należącym do stoplist'y - słowa należące do stoplist'y nie podlegają indeksacji

--ZAD5
SELECT *
FROM CTX_STOPLISTS;

--System wykorzysywał listę określoną jako DEFAULT_STOPLIST

--ZAD6
SELECT *
FROM CTX_STOPWORDS;

--ZAD7
DROP INDEX QUOTES_IDX;

CREATE INDEX QUOTES_IDX ON QUOTES(TEXT) INDEXTYPE IS CTXSYS.CONTEXT PARAMETERS ('stoplist CTXSYS.EMPTY_STOPLIST');

--ZAD8
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'it') > 0;

--Zapytanie tym razem zwróciło wyniki

--ZAD9
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'fool and humans') > 0;

--ZAD10
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'fool and computer') > 0;

--ZAD11
SELECT AUTHOR, TEXT
FROM QUOTES
WHERE CONTAINS(TEXT, '(FOOL AND COMPUTER) WITHIN SENTENCE', 1) > 0;

--Błąd wynika z tego powodu, że sekcja SENTENCE nie została dodana do grupy sekcji

--ZAD12
DROP INDEX QUOTES_IDX;

--ZAD13
BEGIN
    CTX_DDL.CREATE_SECTION_GROUP('nullgroup', 'NULL_SECTION_GROUP');
    CTX_DDL.ADD_SPECIAL_SECTION('nullgroup', 'SENTENCE');
    CTX_DDL.ADD_SPECIAL_SECTION('nullgroup', 'PARAGRAPH');
END;

--ZAD14
CREATE INDEX QUOTES_IDX ON QUOTES(TEXT) INDEXTYPE IS CTXSYS.CONTEXT PARAMETERS ('stoplist CTXSYS.EMPTY_STOPLIST section group nullgroup');

--ZAD15
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, '(fool and humans) WITHIN SENTENCE') > 0;

SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, '(fool and computer) WITHIN SENTENCE') > 0;

--ZAD16
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'humans') > 0;

--System zwrócił też cytaty zawierające 'non-humans', ponieważ przy tworzeniu indeksu nie zdefiniowaliśmy odpowiedniego lexer'a, który potraktowałby znak niealfanumeryczny ('-' między słowami) jako znak alfanumeryczny (składnik słów)

--ZAD17
DROP INDEX QUOTES_IDX;

BEGIN
    CTX_DDL.CREATE_PREFERENCE('lex_z_m', 'BASIC_LEXER');
    CTX_DDL.SET_ATTRIBUTE('lex_z_m', 'printjoins', '-');
    CTX_DDL.SET_ATTRIBUTE('lex_z_m', 'index_text', 'YES');
END;

CREATE INDEX QUOTES_IDX ON QUOTES(TEXT) INDEXTYPE IS CTXSYS.CONTEXT PARAMETERS ('stoplist CTXSYS.EMPTY_STOPLIST section group nullgroup LEXER lex_z_m');

--ZAD18
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'humans') > 0;

--Tym razem system nie zwrócił cytatów zawierających 'non-humans'

--ZAD19
SELECT *
FROM QUOTES
WHERE CONTAINS(TEXT, 'non\-humans') > 0;

--ZAD20
BEGIN
    CTX_DDL.DROP_PREFERENCE('lex_z_m');
    CTX_DDL.DROP_SECTION_GROUP('nullgroup');
END;

DROP TABLE QUOTES;