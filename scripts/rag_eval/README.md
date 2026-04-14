# RAG 召回率离线评测

不修改前后端业务代码，在本目录用 Python 复刻 `FaqRetrieverService` 的：

- FAQ 按 `## ` 切分（与 Java 相同正则）
- 调用同一 OpenAI 兼容 **`/v1/embeddings`** 接口
- 余弦相似度 Top-K + `min_score` 过滤后，判断 **gold 段落标题**是否出现在召回结果中（Hit@K）

## 依赖

```bash
pip install requests
```

（脚本仅用标准库 + `requests` 发 HTTPS，无需 numpy。）

## 环境变量

| 变量 | 说明 |
|------|------|
| `LIFE_REVIEW_API_KEY` | 必填，与后端 `app.ai.api-key` 相同（用于 Embedding） |
| `LIFE_REVIEW_BASE_URL` | 可选，默认 `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `EMBEDDING_MODEL` | 可选，默认 `text-embedding-v3`（与 `app.rag.embedding-model` 一致） |
| `FAQ_PATH` | 可选，默认仓库内 `backend/src/main/resources/rag/faq.md` |
| `GOLD_PATH` | 可选，默认本目录 `gold.json` |
| `TOP_K` | 可选，默认 `3`（`app.rag.top-k`） |
| `MIN_SCORE` | 可选，默认 `0.5`（`app.rag.min-score`） |

## 运行

在项目根目录：

```bash
cd scripts/rag_eval
set LIFE_REVIEW_API_KEY=你的密钥
python rag_recall_eval.py
```

Linux/macOS：

```bash
export LIFE_REVIEW_API_KEY=你的密钥
python3 rag_recall_eval.py
```

## 自定义测试集

编辑 `gold.json`：每条为 `query`（用户问题）与 `gold_heading`（对应 FAQ 中 `## ` 后的标题文本，需与 `faq.md` 完全一致）。

## 指标说明

输出中的 **Recall / Hit@K** 表示：在应用 `MIN_SCORE` 过滤后的 Top-K 条结果中，是否至少有一条段落的标题等于 `gold_heading`。与线上一致时需保证模型、FAQ 文件、`TOP_K`、`MIN_SCORE` 相同。
