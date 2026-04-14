#!/usr/bin/env python3
"""
离线评测 RAG 召回率（不依赖 Spring、不修改业务代码）。

复刻 FaqRetrieverService 的 FAQ 切分与余弦相似度 TopK + min_score 过滤，
通过 OpenAI 兼容 /v1/embeddings 调用与线上一致的向量模型。

用法：
  set LIFE_REVIEW_API_KEY=你的key
  python rag_recall_eval.py

依赖：仅标准库 + requests（pip install requests）
"""

from __future__ import annotations

import json
import math
import os
import re
import sys
from pathlib import Path

import requests

SECTION_SPLIT = re.compile(r"(?=^## )", re.MULTILINE)


def split_faq_sections(markdown: str) -> list[str]:
    """与 FaqRetrieverService.splitFaqSections 一致。"""
    parts = SECTION_SPLIT.split(markdown)
    segments: list[str] = []
    for part in parts:
        trimmed = part.strip()
        if not trimmed:
            continue
        if trimmed.startswith("# ") and not trimmed.startswith("## "):
            continue
        segments.append(trimmed)
    return segments


def heading_of_segment(segment: str) -> str:
    """段落首行 ## 后的标题（用于与 gold 比对）。"""
    first = segment.split("\n", 1)[0].strip()
    if first.startswith("## "):
        return first[3:].strip()
    return first


def l2_norm(v: list[float]) -> float:
    return math.sqrt(sum(x * x for x in v))


def l2_normalize(v: list[float]) -> list[float]:
    n = l2_norm(v)
    if n == 0:
        return list(v)
    return [x / n for x in v]


def cosine_sim(a: list[float], b: list[float]) -> float:
    """余弦相似度（向量未预先归一化时）。"""
    na = l2_norm(a)
    nb = l2_norm(b)
    if na == 0 or nb == 0:
        return 0.0
    dot = sum(x * y for x, y in zip(a, b))
    return dot / (na * nb)


def embed_one(text: str, base_url: str, api_key: str, model: str) -> list[float]:
    url = base_url.rstrip("/") + "/embeddings"
    r = requests.post(
        url,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        json={"model": model, "input": text},
        timeout=120,
    )
    r.raise_for_status()
    data = r.json()
    return [float(x) for x in data["data"][0]["embedding"]]


def retrieve(
    query_emb: list[float],
    segments: list[str],
    segment_embs: list[list[float]],
    top_k: int,
    min_score: float,
) -> list[tuple[str, float]]:
    scores = [cosine_sim(query_emb, se) for se in segment_embs]
    indexed = sorted(enumerate(scores), key=lambda x: -x[1])
    out: list[tuple[str, float]] = []
    for idx, sc in indexed:
        if sc < min_score:
            continue
        out.append((segments[idx], sc))
        if len(out) >= top_k:
            break
    return out


def main() -> int:
    here = Path(__file__).resolve().parent
    root = here.parent.parent
    faq_path = Path(os.environ.get("FAQ_PATH", root / "backend/src/main/resources/rag/faq.md"))
    gold_path = Path(os.environ.get("GOLD_PATH", here / "gold.json"))
    top_k = int(os.environ.get("TOP_K", "1"))
    min_score = float(os.environ.get("MIN_SCORE", "0.5"))

    api_key = os.environ.get("LIFE_REVIEW_API_KEY", "").strip()
    base_url = os.environ.get(
        "LIFE_REVIEW_BASE_URL",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
    ).strip()
    model = os.environ.get("EMBEDDING_MODEL", "text-embedding-v3").strip()

    if not api_key:
        print("请设置环境变量 LIFE_REVIEW_API_KEY（与后端 app.ai.api-key 一致即可）", file=sys.stderr)
        return 2

    if not faq_path.is_file():
        print(f"找不到 FAQ 文件: {faq_path}", file=sys.stderr)
        return 2

    md = faq_path.read_text(encoding="utf-8")
    segments = split_faq_sections(md)
    if not segments:
        print("FAQ 切分后为空", file=sys.stderr)
        return 2

    print(f"FAQ 段落数: {len(segments)}  文件: {faq_path}")
    print(f"模型: {model}  TOP_K={top_k}  MIN_SCORE={min_score} API_KEY={api_key}")
    print("正在向量化段落...")

    segment_embs: list[list[float]] = []
    for i, seg in enumerate(segments):
        segment_embs.append(embed_one(seg, base_url, api_key, model))
        if (i + 1) % 5 == 0 or i + 1 == len(segments):
            print(f"  已索引 {i + 1}/{len(segments)}")

    with gold_path.open(encoding="utf-8") as f:
        cases = json.load(f)

    hits = 0
    total = len(cases)
    details: list[dict] = []

    for row in cases:
        q = row["query"]
        gold_h = row["gold_heading"]
        q_emb = embed_one(q, base_url, api_key, model)
        results = retrieve(q_emb, segments, segment_embs, top_k, min_score)
        heads = [heading_of_segment(t) for t, _ in results]
        hit = gold_h in heads
        if hit:
            hits += 1
        details.append(
            {
                "query": q,
                "gold_heading": gold_h,
                "hit": hit,
                "retrieved_headings": heads,
                "scores": [round(s, 4) for _, s in results],
            }
        )

    recall = hits / total if total else 0.0
    print()
    print("========== RAG 召回评测（Hit@K，K=TOP_K，且通过 MIN_SCORE）==========")
    print(f"用例数: {total}  命中数: {hits}  Recall / Hit@K: {recall:.2%}")
    print()
    for d in details:
        status = "OK " if d["hit"] else "MISS"
        print(f"[{status}] Q: {d['query']}")
        print(f"      期望段落标题: {d['gold_heading']}")
        print(f"      实际召回标题: {d['retrieved_headings']}  scores={d['scores']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
