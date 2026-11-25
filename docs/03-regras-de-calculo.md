# 3. Regras de cálculo do frete

Todas as contas ficam na classe `CalculadoraFrete`. Os valores usam `BigDecimal` com 2 casas e arredondamento HALF_UP (RN12).

## 3.1 Fórmula

```
base        = distância (km) × valor por km do tipo de carga        (RN07, RN08)
especial    = 40% da base, se a carga for frágil ou perigosa         (RN09)
urgência    = 30% da base, se a entrega for urgente                  (RN10)
subtotal    = base + especial + urgência
desconto    = subtotal × percentual do cliente                       (RN01, RN02)
frete       = subtotal − desconto
              se frete < frete mínimo do contrato → frete = mínimo   (RN11)
atraso      = dias entre o prazo limite e a data da entrega           (RN24)
abatimento  = frete × mín(5% × dias de atraso, 25%)                   (RN25)
valor final = frete − abatimento
```

| Tipo de carga | Peso | R$/km | CNH mínima |
|---|---|---|---|
| Leve | até 500 kg | 1,50 | B |
| Média | de 500,1 até 3.000 kg | 2,50 | B |
| Pesada | acima de 3.000 kg | 4,00 | C |

| Tipo de cliente | Entrega regular | Entrega urgente |
|---|---|---|
| Empresarial | 10% de desconto | não permitida |
| Prioritário | 20% de desconto | 20% de desconto |

## 3.2 Exemplos resolvidos

Os exemplos 1 a 4 estão nos testes (`CalculadoraFreteTest`), e o 5 nos dados de demonstração.

### Exemplo 1: carga leve, cliente empresarial
200 kg, carga comum, 100 km.

| Item | Conta | Valor |
|---|---|---|
| Base | 100 × 1,50 | R$ 150,00 |
| Desconto 10% | 150,00 × 0,10 | − R$ 15,00 |
| **Frete** | | **R$ 135,00** |

### Exemplo 2: carga pesada, perigosa e urgente, cliente prioritário
5.000 kg, carga perigosa, 300 km, urgente.

| Item | Conta | Valor |
|---|---|---|
| Base | 300 × 4,00 | R$ 1.200,00 |
| Especial 40% | 1.200 × 0,40 | + R$ 480,00 |
| Urgência 30% | 1.200 × 0,30 | + R$ 360,00 |
| Subtotal | | R$ 2.040,00 |
| Desconto 20% | 2.040 × 0,20 | − R$ 408,00 |
| **Frete** | | **R$ 1.632,00** |

### Exemplo 3: frete mínimo
5 kg, 10 km, cliente empresarial com contrato de frete mínimo R$ 300,00.

| Item | Conta | Valor |
|---|---|---|
| Base | 10 × 1,50 | R$ 15,00 |
| Desconto 10% | | − R$ 1,50 |
| Frete calculado | | R$ 13,50 |
| Ajuste ao mínimo | 300,00 − 13,50 | + R$ 286,50 |
| **Frete** | | **R$ 300,00** |

### Exemplo 4: atraso acima do limite
Frete de R$ 1.350,00, entregue com 12 dias de atraso.
12 × 5% = 60%, mas o limite é 25% → abatimento de R$ 337,50 → **valor final R$ 1.012,50**.

### Exemplo 5: entrega #3 dos dados de demonstração
Construmax (empresarial), 22.000 kg de vergalhões e cimento, 560 km, prazo do contrato de 7 dias.

| Item | Conta | Valor |
|---|---|---|
| Base | 560 × 4,00 | R$ 2.240,00 |
| Desconto 10% | | − R$ 224,00 |
| Frete | | R$ 2.016,00 |
| Prazo | saída + 7 dias | |
| Atraso | entregue 3 dias depois do prazo → 15% | − R$ 302,40 |
| **Valor da nota** | | **R$ 1.713,60** |

## 3.3 Agenda (RN18, RN19)

```
dias de viagem = arredondar para cima(distância ÷ 500), mínimo 1
reserva        = do dia de saída até (saída + dias de viagem − 1)
conflito       = existe outra entrega AGENDADA ou EM TRÂNSITO, do mesmo motorista
                 ou do mesmo veículo, cujo período se sobrepõe
```

Exemplo: saída em 01/12 com 1.200 km → 3 dias → motorista e veículo reservados em 01, 02 e 03/12. Uma entrega cancelada libera a agenda na hora.
