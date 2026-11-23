const form = document.querySelector("#analysis-form");
const resumeInput = document.querySelector("#resume");
const jobInput = document.querySelector("#job");
const jobCount = document.querySelector("#job-count");
const submit = document.querySelector("#submit");
const errorBox = document.querySelector("#error");
const result = document.querySelector("#result");
const scoreElement = document.querySelector("#score");
const scoreBar = document.querySelector(".score-bar");
const scoreFill = document.querySelector("#score-fill");
const summary = document.querySelector("#summary");
const recognized = document.querySelector("#recognized");
const groups = document.querySelector("#groups");
const again = document.querySelector("#again");

const GROUPS = [
    {
        status: "MATCH",
        title: "Encontrados",
        className: "match",
        note: "O requisito aparece numa frase com ação."
    },
    {
        status: "PARTIAL",
        title: "Parciais",
        className: "partial",
        note: "O requisito aparece, mas só em lista ou sem uma frase de ação."
    },
    {
        status: "MISSING",
        title: "Ausentes",
        className: "missing",
        note: "Nenhuma evidência encontrada no currículo."
    }
];

jobInput.addEventListener("input", () => {
    jobCount.textContent = jobInput.value.length.toLocaleString("pt-BR");
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideError();

    const file = resumeInput.files[0];
    const job = jobInput.value.trim();

    if (!file) {
        showError("Escolha o currículo em PDF.");
        return;
    }
    if (!job) {
        showError("Cole a descrição da vaga.");
        return;
    }
    if (job.length > 50000) {
        showError("A descrição da vaga passa de 50.000 caracteres.");
        return;
    }

    setLoading(true);
    try {
        const data = new FormData();
        data.append("resume", file);
        data.append("job", job);

        const response = await fetch("/api/analyze", { method: "POST", body: data });
        const payload = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(payload.detail || "Não foi possível concluir a análise.");
        }
        render(payload);
    } catch (error) {
        showError(error.message || "Erro inesperado.");
    } finally {
        setLoading(false);
    }
});

function render(data) {
    const score = Number(data.score) || 0;
    const requirements = Array.isArray(data.requirements) ? data.requirements : [];

    scoreElement.textContent = formatScore(score);
    scoreFill.style.width = `${Math.min(100, Math.max(0, score))}%`;
    scoreBar.setAttribute("aria-valuenow", String(score));
    scoreBar.dataset.level = score >= 70 ? "high" : score >= 40 ? "medium" : "low";

    summary.textContent = data.summary || "";
    recognized.textContent = `${requirements.length} requisito(s) reconhecido(s) pelo dicionário.`;

    groups.replaceChildren(...GROUPS.map((group) => renderGroup(group, requirements)));
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
        const entry = document.createElement("li");
        entry.className = "requirement";

        const name = document.createElement("h3");
        name.textContent = item.name;
        entry.append(name);

        if (item.evidence) {
            const evidence = document.createElement("blockquote");
            evidence.textContent = item.evidence;
            entry.append(evidence);
        } else {
            const hint = document.createElement("p");
            hint.className = "hint";
            hint.textContent = "Se você realmente tem experiência com este termo, "
                + "considere deixar isso explícito no currículo.";
            entry.append(hint);
        }
        list.append(entry);
    }

    section.append(list);
    return section;
}

function formatScore(score) {
    return Number.isInteger(score) ? String(score) : score.toFixed(1);
}

function setLoading(loading) {
    submit.disabled = loading;
    submit.textContent = loading ? "Analisando..." : "Analisar";
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = false;
}

function hideError() {
    errorBox.hidden = true;
    errorBox.textContent = "";
}

again.addEventListener("click", () => {
    result.hidden = true;
    form.reset();
    jobCount.textContent = "0";
    hideError();
    resumeInput.focus();
});
