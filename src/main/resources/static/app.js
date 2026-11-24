const MAX_PDF_BYTES = 10 * 1024 * 1024;
const MAX_RESUME_CHARS = 200000;
const MAX_JOB_CHARS = 50000;
const ALTERNATIVE_SEPARATOR = " / ";

const form = document.querySelector("#analysis-form");
const tabs = [...document.querySelectorAll(".tab")];
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

let mode = "pdf";
let lastResult = null;

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
    const hasScore = typeof data.score === "number";
    const score = hasScore ? data.score : 0;
    lastResult = { score: hasScore ? score : null, requirements, summary: data.summary || "" };

    scoreElement.textContent = hasScore ? formatScore(score) : "Sem nota";
    scoreElement.parentElement.classList.toggle("no-score", !hasScore);
    scoreMax.hidden = !hasScore;
    tally.hidden = requirements.length === 0;
    scoreFill.style.width = `${Math.min(100, Math.max(0, score))}%`;
    scoreBar.hidden = !hasScore;
    scoreBar.setAttribute("aria-valuenow", String(score));
    scoreBar.dataset.level = score >= 70 ? "high" : score >= 40 ? "medium" : "low";

    tally.replaceChildren(...GROUPS.map((group) => {
        const count = requirements.filter((r) => r.status === group.status).length;
        const item = document.createElement("li");
        item.className = `chip ${group.className}`;
        item.textContent = `${count} ${group.short}`;
        return item;
    }));

    recognized.textContent = requirements.length === 0
        ? "Nenhum requisito da vaga foi reconhecido pelo dicionário, então não há nota."
        : `${requirements.length} requisito(s) reconhecido(s) pelo dicionário.`;
    summary.textContent = data.summary || "";
    copyStatus.textContent = "";

    groups.replaceChildren(...(requirements.length === 0
        ? []
        : GROUPS.map((group) => renderGroup(group, requirements))));
    result.hidden = false;
    result.scrollIntoView({ behavior: "smooth", block: "start" });
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
    const options = String(item.name).split(ALTERNATIVE_SEPARATOR);
    const entry = document.createElement("li");
    entry.className = "requirement";

    const name = document.createElement("h3");
    name.textContent = options.join(" · ");
    if (options.length > 1) {
        const tag = document.createElement("span");
        tag.className = "tag";
        tag.textContent = "qualquer uma";
        tag.title = "A vaga aceita qualquer uma destas tecnologias.";
        name.append(" ", tag);
    }
    entry.append(name);

    if (item.evidence) {
        const evidence = document.createElement("blockquote");
        evidence.append(highlight(item.evidence, options));
        entry.append(evidence);
    } else {
        const hint = document.createElement("p");
        hint.className = "hint";
        hint.textContent = "Se você realmente tem experiência com este termo, "
            + "considere deixar isso explícito no currículo.";
        entry.append(hint);
    }
    return entry;
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
        data.score === null ? "Resume Match: sem nota" : `Resume Match: ${formatScore(data.score)}/100`,
        ""
    ];
    for (const group of GROUPS) {
        const items = data.requirements.filter((r) => r.status === group.status);
        if (items.length === 0) {
            continue;
        }
        lines.push(`${group.title} (${items.length})`);
        for (const item of items) {
            lines.push(item.evidence ? `- ${item.name}: "${item.evidence}"` : `- ${item.name}`);
        }
        lines.push("");
    }
    lines.push(data.summary);
    return lines.join("\n");
}

again.addEventListener("click", () => {
    hideError();
    form.scrollIntoView({ behavior: "smooth", block: "start" });
    jobInput.focus({ preventScroll: true });
});

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
