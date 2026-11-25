const MAX_PDF_BYTES = 10 * 1024 * 1024;
const MAX_RESUME_CHARS = 200000;
const MAX_JOB_CHARS = 50000;
const ALTERNATIVE_SEPARATOR = " / ";
const COMPONENT_SEPARATOR = " + ";

const form = document.querySelector("#analysis-form");
const tabs = [...document.querySelectorAll(".segment")];
const entry = document.querySelector("#entry");
const resultActions = document.querySelector("#result-actions");
const backToResult = document.querySelector("#back-to-result");
const resultTabs = [...document.querySelectorAll(".result-tab")];
const filters = document.querySelector("#filters");
const unevaluatedBox = document.querySelector("#unevaluated");
const countRequirements = document.querySelector("#count-requirements");
const countImprove = document.querySelector("#count-improve");
const countUnevaluated = document.querySelector("#count-unevaluated");
const panelPdf = document.querySelector("#panel-pdf");
const panelText = document.querySelector("#panel-text");
const dropzone = document.querySelector("#dropzone");
const resumeInput = document.querySelector("#resume");
const fileLabel = document.querySelector("#file-label");
const fileDetail = document.querySelector("#file-detail");
const resumeText = document.querySelector("#resume-text");
const resumeCount = document.querySelector("#resume-count");
const jobInput = document.querySelector("#job");
const jobCount = document.querySelector("#job-count");
const submit = document.querySelector("#submit");
const submitLabel = document.querySelector("#submit-label");
const errorBox = document.querySelector("#error");
const result = document.querySelector("#result");
const scoreElement = document.querySelector("#score");
const scoreMax = document.querySelector("#score-max");
const scoreBar = document.querySelector(".score-bar");
const scoreFill = document.querySelector("#score-fill");
const tally = document.querySelector("#tally");
const summary = document.querySelector("#summary");
const recognized = document.querySelector("#recognized");
const groups = document.querySelector("#groups");
const copyButton = document.querySelector("#copy");
const copyStatus = document.querySelector("#copy-status");
const again = document.querySelector("#again");
const feedbackSummary = document.querySelector("#feedback-summary");
const requiredGap = document.querySelector("#required-gap");
const pdfWarning = document.querySelector("#pdf-warning");
const suggestionsList = document.querySelector("#suggestions");
const simulationTotal = document.querySelector("#simulation-total");
const simulationRounding = document.querySelector("#simulation-rounding");
const gapsBox = document.querySelector("#gaps");

/** Mesmo limite do servidor: abaixo disso é ruído do PDF. */
const MIN_HIDDEN_CHARS = 10;

const DEFAULT_FILE_LABEL = fileLabel.textContent;
const DEFAULT_FILE_DETAIL = fileDetail.textContent;

const GROUPS = [
    {
        status: "MATCH",
        title: "Encontrados",
        short: "com ação",
        className: "match",
        note: "O requisito aparece numa frase com ação."
    },
    {
        status: "PARTIAL",
        title: "Parciais",
        short: "só em lista",
        className: "partial",
        note: "O requisito aparece, mas só em lista ou sem uma frase de ação."
    },
    {
        status: "MISSING",
        title: "Ausentes",
        short: "sem evidência",
        className: "missing",
        note: "Nenhuma evidência no currículo, ou o termo só aparece negado."
    }
];

const SECTION_LABELS = {
    REQUIREMENTS: "Requisitos",
    RESPONSIBILITIES: "Responsabilidades",
    PREFERRED: "Diferenciais",
    GENERAL: "Vaga"
};

const REASON_LABELS = {
    EDUCATION: "formação: o programa ainda não compara curso",
    LANGUAGE: "idioma: o programa ainda não compara idioma",
    NO_KNOWN_TERM: "nenhum termo do dicionário"
};

const EVIDENCE_SECTIONS = {
    EXPERIENCE: "Experiência",
    PROJECTS: "Projetos",
    COURSES: "Cursos",
    EDUCATION: "Formação",
    SKILLS: "Competências",
    LANGUAGES: "Idiomas",
    SUMMARY: "Resumo",
    HEADER: "Cabeçalho"
};

const SUGGESTION_TYPES = {
    LIST_ONLY: {
        letter: "A",
        label: "só em lista",
        title: "O termo aparece no currículo, mas sem frase de ação."
    },
    INDICATED: {
        letter: "B",
        label: "indício",
        title: "O termo falta, mas outro termo do currículo sugere que você pode ter."
    }
};

/**
 * Cada ponto e o total saem da cobertura arredondada em uma casa, então somar
 * as sugestões pode dar alguns décimos a mais ou a menos que o total.
 */
const ROUNDING_NOTE = "Valores arredondados: a soma das sugestões pode diferir do total em alguns décimos.";

const STATUS_MARKS = { MATCH: "✓", PARTIAL: "~", MISSING: "✗" };
const VERDICT_LABELS = { MATCH: "encontrado", PARTIAL: "parcial", MISSING: "ausente" };

let mode = "pdf";
let lastResult = null;
let analysisId = null;
let statusFilter = "ALL";

/* ---------- entrada ---------- */

tabs.forEach((tab) => {
    tab.addEventListener("click", () => selectMode(tab.id === "tab-text" ? "text" : "pdf"));
    tab.addEventListener("keydown", (event) => {
        if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
            event.preventDefault();
            selectMode(mode === "pdf" ? "text" : "pdf");
            tabs.find((t) => t.getAttribute("aria-selected") === "true").focus();
        }
    });
});

function selectMode(next) {
    mode = next;
    tabs.forEach((tab) => {
        const selected = (tab.id === "tab-text") === (next === "text");
        tab.setAttribute("aria-selected", String(selected));
        tab.tabIndex = selected ? 0 : -1;
    });
    panelPdf.hidden = next !== "pdf";
    panelText.hidden = next !== "text";
    hideError();
}

resumeInput.addEventListener("change", () => showFile(resumeInput.files[0]));

["dragenter", "dragover"].forEach((type) => {
    dropzone.addEventListener(type, (event) => {
        event.preventDefault();
        dropzone.classList.add("dragging");
    });
});

["dragleave", "drop"].forEach((type) => {
    dropzone.addEventListener(type, () => dropzone.classList.remove("dragging"));
});

dropzone.addEventListener("drop", (event) => {
    event.preventDefault();
    const files = event.dataTransfer && event.dataTransfer.files;
    if (files && files.length > 0) {
        resumeInput.files = files;
        showFile(files[0]);
    }
});

function showFile(file) {
    if (!file) {
        fileLabel.textContent = DEFAULT_FILE_LABEL;
        fileDetail.textContent = DEFAULT_FILE_DETAIL;
        dropzone.classList.remove("has-file");
        return;
    }
    fileLabel.textContent = file.name;
    fileDetail.textContent = `${formatSize(file.size)} · clique para trocar`;
    dropzone.classList.add("has-file");
    hideError();
}

resumeText.addEventListener("input", () => {
    resumeCount.textContent = resumeText.value.length.toLocaleString("pt-BR");
});

jobInput.addEventListener("input", () => {
    jobCount.textContent = jobInput.value.length.toLocaleString("pt-BR");
});

form.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
        event.preventDefault();
        form.requestSubmit();
    }
});

/* ---------- envio ---------- */

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideError();

    const job = jobInput.value.trim();
    const problem = validate(job);
    if (problem) {
        showError(problem);
        return;
    }

    setLoading(true);
    try {
        const response = mode === "pdf"
            ? await fetch("/api/analyze", { method: "POST", body: pdfForm(job) })
            : await fetch("/api/analyze/text", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ resume: resumeText.value, job })
            });
        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(payload.detail || "Não foi possível concluir a análise.");
        }
        render(payload);
    } catch (error) {
        showError(error instanceof TypeError
            ? "Não foi possível falar com o servidor. Ele está rodando?"
            : error.message || "Erro inesperado.");
    } finally {
        setLoading(false);
    }
});

function validate(job) {
    if (mode === "pdf") {
        const file = resumeInput.files[0];
        if (!file) {
            return "Escolha o currículo em PDF.";
        }
        if (!/\.pdf$/i.test(file.name) && file.type !== "application/pdf") {
            return "O arquivo escolhido não parece ser um PDF.";
        }
        if (file.size > MAX_PDF_BYTES) {
            return `O PDF tem ${formatSize(file.size)} e o limite é 10 MB.`;
        }
    } else {
        if (!resumeText.value.trim()) {
            return "Cole o texto do currículo.";
        }
        if (resumeText.value.length > MAX_RESUME_CHARS) {
            return "O texto do currículo passa de 200.000 caracteres.";
        }
    }
    if (!job) {
        return "Cole a descrição da vaga.";
    }
    if (job.length > MAX_JOB_CHARS) {
        return "A descrição da vaga passa de 50.000 caracteres.";
    }
    return null;
}

function pdfForm(job) {
    const data = new FormData();
    data.append("resume", resumeInput.files[0]);
    data.append("job", job);
    return data;
}

/* ---------- resultado ---------- */

function render(data) {
    const requirements = Array.isArray(data.requirements) ? data.requirements : [];
    const unevaluated = Array.isArray(data.unevaluated) ? data.unevaluated : [];
    const unknownTerms = Array.isArray(data.unknownTerms) ? data.unknownTerms : [];
    const hasScore = typeof data.score === "number";
    const score = hasScore ? data.score : 0;
    analysisId = newAnalysisId();
    loadFeedbackSummary();
    lastResult = {
        pdfWarning: pdfWarningText(data.pdfCheck),
        score: hasScore ? score : null,
        requirements,
        unevaluated,
        unknownTerms,
        improvement: data.improvement || null,
        summary: data.summary || ""
    };

    scoreElement.textContent = hasScore ? formatScore(score) : "Sem cálculo";
    requiredGap.textContent = requiredGapText(requirements);
    const warning = pdfWarningText(data.pdfCheck);
    pdfWarning.textContent = warning;
    pdfWarning.hidden = !warning;
    scoreElement.parentElement.classList.toggle("no-score", !hasScore);
    scoreMax.hidden = !hasScore;
    tally.hidden = requirements.length === 0;
    scoreFill.style.transform = "scaleX(0)";
    scoreBar.hidden = !hasScore;
    scoreBar.setAttribute("aria-valuenow", String(score));
    scoreBar.dataset.level = score >= 70 ? "high" : score >= 40 ? "medium" : "low";

    tally.replaceChildren(...GROUPS.map((group) => {
        const count = requirements.filter((r) => r.status === group.status).length;
        const item = document.createElement("li");
        const button = document.createElement("button");
        button.type = "button";
        button.className = `chip ${group.className}`;
        button.textContent = `${count} ${group.short}`;
        button.title = `Ver só os ${group.title.toLowerCase()}`;
        button.addEventListener("click", () => {
            setStatusFilter(group.status);
            selectResultTab("tab-requirements");
        });
        item.append(button);
        return item;
    }));

    recognized.textContent = requirements.length === 0
        ? "Nenhum requisito da vaga foi reconhecido pelo dicionário, então não há cálculo."
        : `${requirements.length} requisito(s) reconhecido(s) pelo dicionário.`;
    summary.textContent = data.summary || "";
    copyStatus.textContent = "";

    countRequirements.textContent = String(requirements.length);
    statusFilter = "ALL";
    renderRequirements();
    renderSimulation(data.improvement, hasScore ? score : null);
    countUnevaluated.textContent = String(unevaluated.length + unknownTerms.length);
    unevaluatedBox.replaceChildren(renderUnevaluated(data.unevaluated || [], data.unknownTerms || []));

    selectResultTab("tab-requirements");
    showResult();
    void scoreFill.offsetWidth;
    scoreFill.style.transform = `scaleX(${Math.min(100, Math.max(0, score)) / 100})`;
}

/* ---------- etapas e abas ---------- */

/** O resultado ocupa a tela; o formulário fica guardado para editar. */
function showResult() {
    entry.hidden = true;
    result.hidden = false;
    resultActions.hidden = false;
    window.scrollTo({ top: 0 });
    result.focus({ preventScroll: true });
}

function showEntry() {
    hideError();
    result.hidden = true;
    resultActions.hidden = true;
    entry.hidden = false;
    backToResult.hidden = lastResult === null;
    window.scrollTo({ top: 0 });
    jobInput.focus({ preventScroll: true });
}

again.addEventListener("click", showEntry);
backToResult.addEventListener("click", showResult);

resultTabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectResultTab(tab.id));
    tab.addEventListener("keydown", (event) => {
        const last = resultTabs.length - 1;
        const next = {
            ArrowRight: index === last ? 0 : index + 1,
            ArrowLeft: index === 0 ? last : index - 1,
            Home: 0,
            End: last
        }[event.key];
        if (next !== undefined) {
            event.preventDefault();
            selectResultTab(resultTabs[next].id);
            resultTabs[next].focus();
        }
    });
});

function selectResultTab(id) {
    resultTabs.forEach((tab) => {
        const selected = tab.id === id;
        tab.setAttribute("aria-selected", String(selected));
        tab.tabIndex = selected ? 0 : -1;
        document.getElementById(tab.getAttribute("aria-controls")).hidden = !selected;
    });
}

/** Botões "Todos / Encontrados / Parciais / Ausentes" acima da lista. */
function renderFilters(requirements) {
    const options = [
        { status: "ALL", title: "Todos", className: "", count: requirements.length },
        ...GROUPS.map((group) => ({
            status: group.status,
            title: group.title,
            className: group.className,
            count: requirements.filter((r) => r.status === group.status).length
        }))
    ];
    filters.hidden = requirements.length === 0;
    filters.replaceChildren(...options.map((option) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `filter ${option.className}`;
        button.setAttribute("aria-pressed", String(option.status === statusFilter));
        button.append(option.title, " ");
        const count = document.createElement("span");
        count.className = "count";
        count.textContent = String(option.count);
        button.append(count);
        button.addEventListener("click", () => setStatusFilter(option.status));
        return button;
    }));
}

function setStatusFilter(status) {
    statusFilter = status;
    renderRequirements();
}

function renderRequirements() {
    const requirements = lastResult ? lastResult.requirements : [];
    renderFilters(requirements);
    if (requirements.length === 0) {
        const empty = document.createElement("p");
        empty.className = "empty";
        empty.textContent = "Nenhum requisito da vaga foi reconhecido pelo dicionário, "
            + "então não há cálculo. Veja a aba \"Não avaliado\".";
        groups.replaceChildren(empty);
        return;
    }
    const shown = statusFilter === "ALL"
        ? GROUPS.filter((group) => requirements.some((r) => r.status === group.status))
        : GROUPS.filter((group) => group.status === statusFilter);
    groups.replaceChildren(...shown.map((group) => renderGroup(group, requirements)));
}

function renderGroup(group, requirements) {
    const items = requirements.filter((requirement) => requirement.status === group.status);
    const section = document.createElement("section");
    section.className = `group ${group.className}`;

    const header = document.createElement("div");
    header.className = "group-header";

    const title = document.createElement("h2");
    title.textContent = `${group.title} (${items.length})`;

    const note = document.createElement("p");
    note.className = "hint";
    note.textContent = group.note;

    header.append(title, note);
    section.append(header);

    if (items.length === 0) {
        const empty = document.createElement("p");
        empty.className = "empty";
        empty.textContent = "Nenhum requisito nesta categoria.";
        section.append(empty);
        return section;
    }

    const list = document.createElement("ul");
    list.className = "requirement-list";
    for (const item of items) {
        list.append(renderRequirement(item));
    }
    section.append(list);
    return section;
}

function renderRequirement(item) {
    const components = Array.isArray(item.components) ? item.components : [];
    const options = components.length > 0
        ? components.map((component) => component.name)
        : String(item.name).split(ALTERNATIVE_SEPARATOR);
    const entry = document.createElement("li");
    entry.className = "requirement";

    const name = document.createElement("h3");
    name.textContent = options.join(" · ");
    if (components.length > 0) {
        name.append(" ", tag("conjunto",
            "Todas as partes com frase de ação: encontrado. Só algumas: parcial."));
    } else if (options.length > 1) {
        name.append(" ", tag("qualquer uma", "A vaga aceita qualquer uma destas tecnologias."));
    }
    if (item.priority === "PREFERRED") {
        name.append(" ", tag("diferencial", "Diferencial: vale metade no score."));
    }
    entry.append(name);

    if (components.length > 0) {
        const parts = document.createElement("ul");
        parts.className = "parts";
        parts.setAttribute("aria-label", "Partes do conjunto");
        for (const component of components) {
            const part = document.createElement("li");
            const group = GROUPS.find((g) => g.status === component.status);
            part.className = `chip ${group ? group.className : ""}`;
            part.textContent = `${STATUS_MARKS[component.status] || ""} ${component.name}`;
            part.title = group ? group.title : "";
            parts.append(part);
        }
        entry.append(parts);
    }

    if (item.evidence) {
        const evidence = document.createElement("blockquote");
        const section = EVIDENCE_SECTIONS[item.evidenceSection];
        if (section) {
            evidence.append(tag(section, "Seção do currículo de onde veio a evidência"), " ");
        }
        evidence.append(highlight(item.evidence, options));
        entry.append(evidence);
    } else {
        const hint = document.createElement("p");
        hint.className = "hint";
        hint.textContent = "Nenhum trecho do currículo comprova este requisito.";
        entry.append(hint);
    }
    entry.append(renderVerdict(item));
    return entry;
}

/**
 * Botões para marcar a decisão como certa ou dizer qual seria a certa. Cada
 * marcação vai para o feedback.csv local e entra no acerto do programa.
 */
function renderVerdict(item) {
    const verdict = document.createElement("div");
    verdict.className = "verdict";
    const question = document.createElement("span");
    question.className = "hint";
    question.textContent = "A decisão está certa?";
    verdict.append(question);

    const status = document.createElement("span");
    status.className = "hint verdict-status";
    status.setAttribute("role", "status");
    const order = [item.status, ...GROUPS.map((g) => g.status).filter((s) => s !== item.status)];
    const buttons = order.map((correctStatus) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "secondary small";
        button.textContent = correctStatus === item.status
            ? "Certo"
            : `Devia ser ${VERDICT_LABELS[correctStatus]}`;
        button.setAttribute("aria-pressed", "false");
        button.addEventListener("click", () => sendVerdict(item, correctStatus, button, buttons, status));
        return button;
    });
    verdict.append(...buttons, status);
    return verdict;
}

async function sendVerdict(item, correctStatus, button, buttons, status) {
    status.textContent = "";
    try {
        const response = await fetch("/api/feedback", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                analysisId,
                requirement: item.name,
                programStatus: item.status,
                correctStatus
            })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.detail || "Não foi possível salvar a marcação.");
        }
        buttons.forEach((b) => b.setAttribute("aria-pressed", String(b === button)));
        status.textContent = "Marcado.";
        showFeedbackSummary(payload);
    } catch (error) {
        status.textContent = error.message || "Não foi possível salvar a marcação.";
    }
}

async function loadFeedbackSummary() {
    feedbackSummary.textContent = "";
    try {
        const response = await fetch("/api/feedback/summary");
        if (response.ok) {
            showFeedbackSummary(await response.json());
        }
    } catch {
        feedbackSummary.textContent = "";
    }
}

function showFeedbackSummary(metrics) {
    if (!metrics || !metrics.total) {
        feedbackSummary.textContent =
            "Marque cada decisão como certa ou errada para medir o acerto do programa.";
        return;
    }
    const match = metrics.byStatus && metrics.byStatus.MATCH;
    const precision = match && typeof match.precision === "number"
        ? ` "Encontrado" certo em ${formatScore(match.precision)}% das vezes.`
        : "";
    feedbackSummary.textContent = `Suas marcações: ${metrics.total} decisão(ões), `
        + `${metrics.correct} certa(s) (${formatScore(metrics.accuracy)}%).${precision}`;
}

function newAnalysisId() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
        return window.crypto.randomUUID();
    }
    return `a-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

/**
 * Texto que uma pessoa não vê no PDF. Invisível e transparente ficaram fora da
 * análise; branco, minúsculo e fora da página entraram, mas podem ser legítimos.
 */
function pdfWarningText(check) {
    if (!check) {
        return "";
    }
    if (check.scannedLayer) {
        return "O PDF parece escaneado com camada de texto (OCR): o texto fica invisível "
            + "por cima da imagem. A análise usou essa camada.";
    }
    const parts = [];
    if (check.invisibleChars >= MIN_HIDDEN_CHARS) {
        parts.push(`O PDF tinha ${check.invisibleChars} caracteres invisíveis ou transparentes, `
            + `que ficaram fora da análise: "${check.invisibleSample}".`);
    }
    const suspicious = [
        check.whiteChars >= MIN_HIDDEN_CHARS ? `${check.whiteChars} em branco` : null,
        check.tinyChars >= MIN_HIDDEN_CHARS ? `${check.tinyChars} em fonte minúscula` : null,
        check.outsideChars >= MIN_HIDDEN_CHARS ? `${check.outsideChars} fora da página` : null
    ].filter(Boolean);
    if (suspicious.length > 0) {
        parts.push(`O PDF tem caracteres suspeitos: ${suspicious.join(", ")}. Eles entraram na análise: `
            + "pode ser texto escondido ou só letra branca sobre fundo escuro. "
            + `Trecho: "${check.suspiciousSample}".`);
    }
    return parts.join(" ");
}

/** Quantos obrigatórios têm alguma evidência e quais não têm nenhuma. */
function requiredGapText(requirements) {
    const required = requirements.filter((r) => r.priority !== "PREFERRED");
    if (required.length === 0) {
        return "";
    }
    const missing = required.filter((r) => r.status === "MISSING");
    const base = `Obrigatórios com alguma evidência: ${required.length - missing.length} de ${required.length}.`;
    if (missing.length === 0) {
        return base;
    }
    return `${base} Sem evidência: ${missing.map(displayName).join(", ")}.`;
}

function displayName(item) {
    const components = Array.isArray(item.components) ? item.components : [];
    const names = components.length > 0
        ? components.map((component) => component.name)
        : String(item.name).split(ALTERNATIVE_SEPARATOR);
    return names.join(" · ");
}

function tag(text, title) {
    const element = document.createElement("span");
    element.className = "tag";
    element.textContent = text;
    element.title = title;
    return element;
}

/**
 * Mostra quanto a cobertura subiria com cada mudança no currículo. Os pontos
 * vêm prontos do servidor, calculados com a mesma conta da cobertura real.
 */
function renderSimulation(improvement, score) {
    const suggestions = improvement && Array.isArray(improvement.suggestions) ? improvement.suggestions : [];
    const gaps = improvement && Array.isArray(improvement.gaps) ? improvement.gaps : [];
    countImprove.textContent = String(suggestions.length);

    suggestionsList.replaceChildren(...suggestions.map(renderSuggestion));
    suggestionsList.hidden = suggestions.length === 0;
    const hasTotal = score !== null && Boolean(improvement)
        && typeof improvement.scoreWithAll === "number";
    simulationRounding.textContent = ROUNDING_NOTE;
    simulationRounding.hidden = !hasTotal;
    if (score === null || !improvement) {
        simulationTotal.textContent = "Sem requisito reconhecido, não há o que simular.";
    } else if (hasTotal) {
        simulationTotal.textContent = `Fazendo todas: a cobertura iria de ${formatScore(score)} `
            + `para ${formatScore(improvement.scoreWithAll)}/100.`;
    } else if (gaps.length === 0) {
        simulationTotal.textContent = "Todos os requisitos reconhecidos já aparecem numa frase de ação. "
            + "Não há o que simular.";
    } else {
        simulationTotal.textContent = "Nenhuma mudança de escrita aumenta a cobertura: "
            + "o que falta não aparece no currículo.";
    }
    gapsBox.replaceChildren(...(gaps.length > 0 ? [renderGaps(gaps)] : []));
}

function renderSuggestion(item) {
    const type = SUGGESTION_TYPES[item.type] || { letter: "?", label: "", title: "" };
    const spoken = spokenName(item.requirement);
    const entry = document.createElement("li");
    entry.className = "requirement";

    const head = document.createElement("div");
    head.className = "suggestion-head";
    const name = document.createElement("h3");
    name.textContent = requirementNames(item.requirement).join(" · ");
    name.append(" ", tag(`${type.letter} · ${type.label}`, type.title));
    if (item.priority === "PREFERRED") {
        name.append(" ", tag("diferencial", "Diferencial: vale metade no score."));
    }
    const points = document.createElement("span");
    points.className = "points";
    points.textContent = `+${formatScore(item.points)} pontos`;
    points.title = `Só com esta mudança: ${formatScore(item.scoreWithChange)}/100`;
    head.append(name, points);

    const advice = document.createElement("p");
    advice.className = "advice";
    advice.textContent = item.type === "INDICATED"
        ? `Seu currículo tem ${item.indicator}. Se você trabalha com ${spoken}, `
            + `cite ${spoken} numa frase de ação.`
        : `Se você usou ${spoken} em algum projeto, descreva isso numa frase de ação.`;
    entry.append(head, advice);

    if (item.evidence) {
        const evidence = document.createElement("blockquote");
        const names = item.type === "INDICATED" ? [item.indicator] : requirementNames(item.requirement);
        evidence.append("No currículo: ", highlight(item.evidence, names));
        entry.append(evidence);
    }
    return entry;
}

function renderGaps(gaps) {
    const box = document.createElement("div");
    const title = document.createElement("h3");
    title.textContent = `Sem indício no currículo (${gaps.length})`;
    const note = document.createElement("p");
    note.className = "hint";
    note.textContent = "Nenhum termo do currículo indica estes requisitos. "
        + "Ficam fora da simulação e não dão pontos. Se a vaga aceita uma alternativa "
        + "que você já tem, confira na lista de requisitos.";
    const list = document.createElement("ul");
    list.className = "parts";
    for (const gap of gaps) {
        const chip = document.createElement("li");
        chip.className = "chip missing";
        chip.textContent = gapText(gap);
        list.append(chip);
    }
    box.append(title, note, list);
    return box;
}

function gapText(gap) {
    const names = requirementNames(gap.requirement);
    const missing = Array.isArray(gap.missingParts) ? gap.missingParts : [];
    let text = names.join(" · ");
    if (missing.length > 0 && missing.length < names.length) {
        text += ` (falta ${spokenList(missing, "e")})`;
    }
    return gap.priority === "PREFERRED" ? `${text} · diferencial` : text;
}

/** Partes de um conjunto ou opções de alternativas, a partir do nome. */
function requirementNames(name) {
    const text = String(name);
    return text.includes(COMPONENT_SEPARATOR)
        ? text.split(COMPONENT_SEPARATOR)
        : text.split(ALTERNATIVE_SEPARATOR);
}

/** "TCP/IP + DNS" vira "TCP/IP e DNS"; "Azure / AWS" vira "Azure ou AWS". */
function spokenName(name) {
    const text = String(name);
    return text.includes(COMPONENT_SEPARATOR)
        ? spokenList(text.split(COMPONENT_SEPARATOR), "e")
        : spokenList(text.split(ALTERNATIVE_SEPARATOR), "ou");
}

function spokenList(names, conjunction) {
    if (names.length < 2) {
        return names.join("");
    }
    return `${names.slice(0, -1).join(", ")} ${conjunction} ${names.at(-1)}`;
}

/**
 * Mostra o que a vaga pede e o programa não avaliou: trechos sem termo do
 * dicionário, formação, idioma e nomes que o dicionário não conhece.
 */
function renderUnevaluated(items, unknownTerms) {
    const section = document.createElement("div");
    section.className = "unevaluated";

    if (items.length === 0 && unknownTerms.length === 0) {
        const empty = document.createElement("p");
        empty.className = "empty";
        empty.textContent = "Nada ficou de fora: todo trecho de requisito da vaga tinha termo do dicionário.";
        section.append(empty);
        return section;
    }

    if (items.length > 0) {
        const list = document.createElement("ul");
        list.className = "requirement-list";
        for (const item of items) {
            const entry = document.createElement("li");
            entry.className = "requirement";
            const text = document.createElement("p");
            text.className = "unevaluated-text";
            text.textContent = item.text;
            const reason = document.createElement("p");
            reason.className = "hint";
            reason.append(
                tag(SECTION_LABELS[item.section] || "Vaga", "Seção da vaga"),
                ` ${REASON_LABELS[item.reason] || ""}`);
            entry.append(text, reason);
            if (item.resumeEvidence) {
                const found = document.createElement("blockquote");
                found.textContent = `No currículo: ${item.resumeEvidence}`;
                entry.append(found);
            } else if (item.reason === "EDUCATION" || item.reason === "LANGUAGE") {
                const missing = document.createElement("p");
                missing.className = "hint";
                missing.textContent = "Não encontrei isso no currículo.";
                entry.append(missing);
            }
            list.append(entry);
        }
        section.append(list);
    }

    if (unknownTerms.length > 0) {
        const label = document.createElement("p");
        label.className = "hint";
        label.textContent = "Nomes que o dicionário não conhece:";
        const terms = document.createElement("ul");
        terms.className = "parts";
        for (const term of unknownTerms) {
            const chip = document.createElement("li");
            chip.className = "chip";
            chip.textContent = term;
            terms.append(chip);
        }
        section.append(label, terms);
    }
    return section;
}

/**
 * Destaca o nome do requisito dentro da evidência, sem montar HTML a partir do
 * texto: cada pedaço entra como nó de texto. Sinônimos ("k8s" para Kubernetes)
 * e equivalências ("Spring Boot" para Spring) não são destacados.
 */
function highlight(text, names) {
    const fragment = document.createDocumentFragment();
    const terms = names.filter(Boolean).map(escapeRegExp);
    if (terms.length === 0) {
        fragment.append(text);
        return fragment;
    }
    const pattern = new RegExp(`(?<![\\p{L}\\p{N}])(?:${terms.join("|")})(?!\\p{L})`, "giu");
    let last = 0;
    for (const match of text.matchAll(pattern)) {
        fragment.append(text.slice(last, match.index));
        const mark = document.createElement("mark");
        mark.textContent = match[0];
        fragment.append(mark);
        last = match.index + match[0].length;
    }
    fragment.append(text.slice(last));
    return fragment;
}

/* ---------- ações ---------- */

copyButton.addEventListener("click", async () => {
    if (!lastResult) {
        return;
    }
    try {
        await navigator.clipboard.writeText(report(lastResult));
        copyStatus.textContent = "Copiado.";
    } catch {
        copyStatus.textContent = "O navegador não deixou copiar.";
    }
});

function report(data) {
    const lines = [
        data.score === null
            ? "Resume Match: cobertura técnica sem cálculo"
            : `Resume Match: cobertura técnica ${formatScore(data.score)}/100`,
        ""
    ];
    if (data.pdfWarning) {
        lines.push(`Aviso do PDF: ${data.pdfWarning}`, "");
    }
    for (const group of GROUPS) {
        const items = data.requirements.filter((r) => r.status === group.status);
        if (items.length === 0) {
            continue;
        }
        lines.push(`${group.title} (${items.length})`);
        for (const item of items) {
            const name = item.priority === "PREFERRED" ? `${item.name} (diferencial)` : item.name;
            lines.push(item.evidence ? `- ${name}: "${item.evidence}"` : `- ${name}`);
            for (const component of item.components || []) {
                lines.push(`    ${STATUS_MARKS[component.status] || ""} ${component.name}`);
            }
        }
        lines.push("");
    }
    if (data.unevaluated.length > 0) {
        lines.push(`Não avaliado pelo programa (${data.unevaluated.length})`);
        for (const item of data.unevaluated) {
            lines.push(`- ${item.text} [${REASON_LABELS[item.reason] || ""}]`);
            if (item.resumeEvidence) {
                lines.push(`    No currículo: "${item.resumeEvidence}"`);
            }
        }
        lines.push("");
    }
    if (data.unknownTerms.length > 0) {
        lines.push(`Nomes que o dicionário não conhece: ${data.unknownTerms.join(", ")}`, "");
    }
    lines.push(...simulationLines(data.improvement, data.score));
    lines.push(data.summary);
    return lines.join("\n");
}

function simulationLines(improvement, score) {
    if (score === null || !improvement) {
        return [];
    }
    const suggestions = improvement.suggestions || [];
    const gaps = improvement.gaps || [];
    if (suggestions.length === 0 && gaps.length === 0) {
        return [];
    }
    const lines = ["Simulação de melhora (não muda a cobertura; só vale se for verdade)"];
    for (const item of suggestions) {
        const type = SUGGESTION_TYPES[item.type] || { letter: "?" };
        const why = item.type === "INDICATED" ? `indício: ${item.indicator}` : "só em lista";
        lines.push(`- +${formatScore(item.points)} ${spokenName(item.requirement)} [${type.letter}, ${why}]`);
    }
    if (typeof improvement.scoreWithAll === "number") {
        lines.push(`Fazendo todas: cobertura ${formatScore(improvement.scoreWithAll)}/100`);
        lines.push(ROUNDING_NOTE);
    }
    if (gaps.length > 0) {
        lines.push(`Sem indício no currículo: ${gaps.map(gapText).join("; ")}`);
    }
    lines.push("");
    return lines;
}

/* ---------- utilidades ---------- */

function formatScore(score) {
    return Number.isInteger(score) ? String(score) : score.toFixed(1);
}

function formatSize(bytes) {
    if (bytes < 1024 * 1024) {
        return `${Math.max(1, Math.round(bytes / 1024))} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1).replace(".", ",")} MB`;
}

function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function setLoading(loading) {
    submit.disabled = loading;
    form.setAttribute("aria-busy", String(loading));
    submit.classList.toggle("loading", loading);
    submitLabel.textContent = loading ? "Analisando..." : "Analisar";
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = false;
}

function hideError() {
    errorBox.hidden = true;
    errorBox.textContent = "";
}
