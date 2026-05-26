# UI_GUIDELINES.md
> Regras de interface para projetos com UI web, mobile ou desktop. Clareza vence enfeite.

---

## Princípios de UX/UI

| # | Princípio |
|---|---|
| 1 | Clareza e utilidade acima de estética |
| 2 | Leitura e ação devem ser óbvias — sem adivinhar |
| 3 | Responsividade é requisito, não ajuste cosmético |
| 4 | Interface não pode induzir erro perigoso |
| 5 | Acessibilidade é parte da qualidade, não bônus |
| 6 | Interface lenta, confusa ou enganosa aumenta erro humano |
| 7 | Menos é mais: evitar poluição visual |

---

## Regras de construção

- Mobile-first quando o contexto exigir
- Hierarquia visual forte: o que importa mais, mais visível
- Espaçamento consistente com sistema de tokens
- Cada tela tem um objetivo claro e primário
- Estados de erro, loading e vazio são obrigatórios — não opcionais
- Feedback de ação crítica deve ser explícito e imediato
- Ações destrutivas sempre pedem confirmação

---

## Acessibilidade mínima obrigatória

| Item | Detalhe |
|---|---|
| Contraste | mínimo 4.5:1 para texto normal, 3:1 para texto grande (WCAG AA) |
| Foco visível | outline visível em todos os elementos interativos |
| Navegação por teclado | fluxo principal navegável via Tab |
| Labels reais | `<label for="...">` ou `aria-label` em todo input |
| `aria-*` | usar quando elemento customizado não tem semântica nativa |
| Erro em campo | mensagem associada ao campo via `aria-describedby` |
| Não depender apenas de cor | usar ícone, texto ou forma para indicar estado |
| Alt text | toda imagem não decorativa tem `alt` descritivo |

---

## Segurança na interface

- Nunca confiar em dado do cliente para decisão de autorização
- Nunca esconder ação crítica apenas via CSS como substituto de controle real
- Não expor segredo em HTML, `data-*` attributes, `localStorage` ou `sessionStorage` sem justificativa
- Evitar mensagens que facilitem enumeração de usuários (`"Email não cadastrado"` → `"Credenciais inválidas"`)
- Confirmar ações destrutivas: delete, disable, reset, transferência
- Mascarar dados sensíveis na exibição: CPF parcial, cartão, senha
- Não exibir permissões que o usuário não tem (ocultar da UI, mas controle real é no backend)

---

## Primitivos de componente recomendados

Implementar antes de componentes de negócio:

| Primitivo | Variantes |
|---|---|
| `Button` | primary, secondary, danger, ghost, disabled, loading |
| `Input` | default, error, disabled, com label, com helper |
| `Textarea` | default, error, disabled, com contador de caracteres |
| `Select` | default, error, disabled |
| `Checkbox` / `Radio` | default, checked, error, disabled |
| `Badge` | status (info, success, warning, error), size |
| `Card` | default, clickable, highlighted |
| `Modal` / `Drawer` | padrão de confirmação, padrão de formulário |
| `Table` / `List` | paginação, loading, vazio, erro |
| `Alert` / `Toast` | info, success, warning, error |
| `Empty State` | sem dados, sem resultados de busca, sem permissão |
| `Loading State` | skeleton, spinner, progress |

---

## Formulários

| Regra | Detalhe |
|---|---|
| Feedback de erro claro | mensagem próxima ao campo, não apenas no topo |
| Validação local | para UX responsiva — não para segurança |
| Validação no servidor | sempre — independente do frontend |
| Campos obrigatórios | indicados claramente (asterisco + texto) |
| Limitar input no frontend | `maxlength`, `type`, `pattern` quando fizer sentido de UX |
| Não usar placeholder como label | placeholder some ao digitar — usar label separado |
| Não salvar segredo em storage | senha nunca em localStorage, sessionStorage ou cookie não-httpOnly |
| Submit desabilitado durante loading | evitar duplo envio |

---

## Layout e responsividade

Planejar para:

| Breakpoint | Faixa |
|---|---|
| Mobile pequeno | ≤ 375px |
| Mobile grande | 376px – 480px |
| Tablet | 481px – 768px |
| Desktop | 769px – 1280px |
| Widescreen | > 1280px |

Revisar obrigatoriamente:
- Overflow horizontal (especialmente tabelas)
- Filtros colapsando corretamente
- CTAs acessíveis em mobile (tamanho mínimo de toque: 44x44px)
- Densidade informacional adaptada por breakpoint

---

## Performance visual

- Evitar JavaScript desnecessário no carregamento inicial
- Imagens otimizadas: formato moderno (WebP/AVIF), dimensões corretas, lazy loading
- Skeletons para estados de loading — não spinners globais
- Animações não devem atrasar ação principal
- LCP (Largest Contentful Paint) não deve ser sacrificado por elemento visual pesado
- Fontes: usar `font-display: swap`; limitar a 2 famílias

---

## Estados obrigatórios por tela relevante

Toda tela que exibe dados dinâmicos deve ter implementados:

| Estado | Descrição |
|---|---|
| Normal | dados carregados e exibidos |
| Loading | skeleton ou indicador de carregamento |
| Erro | mensagem clara + opção de retry |
| Vazio | sem dados — com call-to-action ou orientação |
| Sucesso | feedback de ação concluída |
| Sem permissão | `403` — mensagem informativa sem vazar detalhes |

---

## Consistência de design

- Tokens de cor e espaçamento centralizados (CSS variables ou design system)
- Variantes documentadas por componente
- Não duplicar componente por pressa — reutilizar ou evoluir o existente
- Nomes de componentes previsíveis e consistentes com o padrão

---

## Hierarquia de informação por tela

```
1. Objetivo primário da tela — imediatamente visível
2. Ação principal — destacada
3. Informações secundárias — subordinadas visualmente
4. Ações destrutivas — isoladas, com confirmação
5. Navegação — consistente e previsível
```

---

## Checklist de interface antes de considerar pronta

- [ ] Estados: normal, loading, erro, vazio implementados
- [ ] Validação dupla: client (UX) + server (obrigatório)
- [ ] Acessibilidade mínima verificada (contraste, labels, foco)
- [ ] Responsividade testada nos breakpoints principais
- [ ] Ações destrutivas com confirmação
- [ ] Dados sensíveis mascarados quando exibidos
- [ ] Mensagens de erro seguras (sem vazar dado interno)
- [ ] Nenhuma lógica crítica de negócio ou autorização na UI

---

## Regra final

Interface boa não é a mais chamativa.

É a que comunica rápido, reduz erro humano e suporta o fluxo do sistema com segurança e clareza.
