# 7. Guia de estudo para a defesa oral

Perguntas que o professor provavelmente vai fazer, com a resposta curta e o lugar do código para mostrar. Vale os dois estudarem tudo, não só a própria parte.

---

## 7.1 Pilares de POO

**1. Onde vocês usaram herança e por quê?**
Onde existe uma relação real de "é um":
- `ClienteEmpresarial` é um `Cliente`, e `Cliente` é uma `Pessoa`.
- `Motorista` também é uma `Pessoa`.
- `RepositorioJson` é um `RepositorioMemoria` que também grava em arquivo.
- Os PDFs herdam de `DocumentoPdf`, os menus de `Menu` e as exceções de `TranslogException`.

O ganho é escrever uma vez só o que é comum: nome, telefone e e-mail com validação; o laço dos menus; o cabeçalho e o rodapé dos PDFs.
*Mostrar:* `modelo/Pessoa.java`, `modelo/Cliente.java`.

**2. Onde está o polimorfismo?**
Na `CalculadoraFrete`, a linha `cliente.percentualDesconto(urgente)`. A calculadora não sabe se o cliente é empresarial ou prioritário: cada subclasse responde com a sua regra. A verificação de urgência funciona igual, com `podeSolicitarUrgencia()`.
*Mostrar:* `servico/CalculadoraFrete.java` e os dois `percentualDesconto` nas subclasses.

**3. E se a empresa criar um terceiro tipo de cliente?**
Basta criar uma subclasse de `Cliente` e um valor novo no enum `TipoCliente`. A calculadora e o agendamento não mudam, porque não têm `if` por tipo de cliente.

**4. Por que `Cliente` é abstrata?**
Porque não existe cliente "genérico" na Translog: todo cliente é empresarial ou prioritário. Uma classe abstrata impede criar um `new Cliente()` e obriga cada subclasse a implementar o desconto e a regra de urgência.

**5. Qual a diferença entre a interface `Repositorio` e a classe abstrata `Cliente`?**
A interface só define **o que** fazer (salvar, buscar, listar), sem código nem atributos. A classe abstrata tem atributos e código pronto (CNPJ, validação) e deixa só uma parte para as subclasses. Usamos interface no repositório porque as implementações (memória, JSON, banco no futuro) não compartilham código obrigatório.

**6. Como vocês aplicaram encapsulamento?**
- Todos os atributos são `private`.
- Os setters validam o valor: CPF, CNPJ, placa, capacidade, prazo, e-mail.
- O status da entrega não tem setter. Ele só muda por `mudarStatus()`, que bloqueia transições inválidas.
- O histórico é devolvido como lista somente leitura (`Collections.unmodifiableList`).
- O tipo da carga nem é um atributo: é calculado a partir do peso.

*Mostrar:* `modelo/Entrega.java` → `mudarStatus`.

**7. O que é o Template Method e onde vocês usaram?**
É quando a classe base define a sequência de passos de um algoritmo e deixa alguns passos para as subclasses. Em `DocumentoPdf.gerar()` (que é `final`), a ordem é sempre: página, cabeçalho, `escreverCorpo()`, rodapé e gravação. Cada PDF implementa só o `escreverCorpo()`. O `Menu.abrir()` segue a mesma ideia com `executar(opcao)`.

---

## 7.2 Regras de negócio

**8. Como é calculado o frete?**
Base (km × valor do tipo de carga), mais 40% se for frágil ou perigosa, mais 30% se for urgente (os dois sobre a base), menos o desconto do cliente e, se ficar abaixo do mínimo do contrato, o frete sobe para o mínimo. Exemplo: 300 km, pesada, perigosa, urgente, prioritário = 1.200 + 480 + 360 = 2.040 − 20% = **R$ 1.632,00**. Ver [03-regras-de-calculo.md](03-regras-de-calculo.md).

**9. Como o sistema decide se a carga é leve, média ou pesada?**
Pelo peso: até 500 kg é leve, até 3.000 kg é média, acima é pesada. Escolhemos assim para ninguém cadastrar uma carga pesada como leve para pagar menos.

**10. Como funciona o bloqueio de datas?**
Cada entrega reserva o motorista e o veículo do dia da saída até o fim estimado da viagem: distância ÷ 500 km por dia, arredondado para cima. No agendamento, o `AgendaServico` procura entregas pendentes do mesmo motorista ou veículo com período sobreposto. Se encontrar, o sistema recusa e sugere a próxima data em que os dois estão livres.

**11. E a multa por atraso?**
Prazo limite = data de saída + prazo do contrato. Ao concluir, se a data de entrega passou do limite, o abatimento é 5% do frete por dia, no máximo 25%. A entrega #3 da demonstração mostra isso: 3 dias de atraso, 15%, de R$ 2.016,00 para R$ 1.713,60.

**12. Por que um cliente empresarial não consegue pedir entrega urgente?**
O enunciado diz que urgentes só podem ser pedidas por prioritários. No console a pergunta nem aparece para empresariais, e o serviço também bloqueia (`verificarUrgencia`), caso alguém chame direto.

**13. Por que não dá para remover um cliente com entregas?**
Porque as notas fiscais e o histórico apontam para ele. A remoção é bloqueada e o sistema orienta a inativar.

---

## 7.3 Técnicas e decisões

**14. Por que `BigDecimal` e não `double`?**
`double` é binário e não representa exatamente valores como 0,1. Em dinheiro isso gera centavos errados. `BigDecimal` faz a conta decimal exata, e usamos 2 casas com arredondamento HALF_UP.

**15. Como o JSON sabe se um cliente é empresarial ou prioritário?**
O Gson sozinho não sabe recriar subclasses. Criamos um adaptador (`GsonFactory.ClienteAdapter`) que grava um campo `"tipo"` e, na leitura, usa esse campo para criar a subclasse certa.

**16. O que acontece se o arquivo de dados sumir ou estiver corrompido?**
Arquivo ausente: o sistema começa vazio e cria o arquivo no primeiro salvamento. Arquivo corrompido: ele é renomeado para `.corrompido-DATA` (nada se perde), o usuário é avisado e o sistema continua.

**17. E se faltar luz no meio da gravação?**
A gravação é feita num arquivo `.tmp`, que depois substitui o original com `Files.move`. Se cair no meio, o arquivo antigo continua inteiro.

**18. Como vocês testam regras que dependem da data de hoje?**
Os serviços recebem um `java.time.Clock` no construtor. No programa, é o relógio do sistema. Nos testes, é um relógio fixo numa data escolhida, então o resultado não muda de um dia para o outro.

**19. Por que a `Entrega` guarda o id do cliente e não o objeto?**
Para o JSON não repetir os dados do cliente em toda entrega. Assim, o cadastro fica num lugar só.

**20. Por que PDFBox e não iText?**
O iText atual usa licença AGPL, que exigiria abrir o código de quem o usa num produto. O PDFBox é Apache 2.0, livre, e dá o mesmo controle do layout.

**21. Como o sistema não quebra quando o usuário digita errado?**
Toda leitura passa pela classe `Console`, que repete a pergunta até receber um valor válido. Os erros de regra viram exceções com mensagem pronta, capturadas no `Menu.abrir()`. Até um erro inesperado é capturado ali, então o menu continua.

**22. O que é a classe `Sistema`?**
É onde todas as dependências são criadas e ligadas: repositórios, serviços, gerador de relatórios. Cada serviço recebe o que precisa pelo construtor (injeção de dependência). Isso permite trocar o JSON pela memória nos testes.

**23. Como vocês resolveram o desafio do Google Drive?**
A pasta de dados é configurável (`--dados=` ou a variável `TRANSLOG_DADOS`). Apontando para a pasta do Google Drive para computador, os arquivos sincronizam sozinhos. A integração direta pela API ficou como melhoria, porque exige credenciais OAuth.

---

## 7.4 Perguntas de "e se"

**24. Se quisessem trocar o JSON por um banco de dados?**
Criaríamos uma classe `RepositorioSql` que implementa `Repositorio<T>` e trocaríamos só a classe `Sistema`. Os serviços não mudam.

**25. Se quisessem uma interface gráfica?**
As regras estão nos serviços, não nos menus. Uma tela JavaFX ou uma API web chamaria os mesmos serviços.

**26. Se o desconto do prioritário mudasse para 25%?**
Mudaria uma constante em `ClientePrioritario` e o teste correspondente.
