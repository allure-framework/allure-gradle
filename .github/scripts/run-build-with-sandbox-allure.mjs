import { spawn } from "node:child_process";
import { randomUUID, createHash } from "node:crypto";
import { mkdirSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "../..");
const sandboxDir = path.join(repoRoot, "sandbox");

function gradleCommand(cwd) {
    const isWindows = process.platform === "win32";
    return {
        command: isWindows ? "cmd.exe" : "./gradlew",
        args: isWindows ? ["/c", "gradlew.bat", "--no-parallel", "build"] : ["--no-parallel", "build"],
        display: path.relative(repoRoot, cwd)
            ? `${path.relative(repoRoot, cwd)}${path.sep}${isWindows ? "gradlew.bat" : "./gradlew"} --no-parallel build`
            : `${isWindows ? "gradlew.bat" : "./gradlew"} --no-parallel build`,
    };
}

function runCommand(cwd) {
    const gradle = gradleCommand(cwd);
    const stdout = [];
    const stderr = [];

    return new Promise((resolve, reject) => {
        const child = spawn(gradle.command, gradle.args, {
            cwd,
            env: process.env,
            stdio: ["ignore", "pipe", "pipe"],
        });

        child.stdout.on("data", (chunk) => {
            process.stdout.write(chunk);
            stdout.push(Buffer.from(chunk));
        });

        child.stderr.on("data", (chunk) => {
            process.stderr.write(chunk);
            stderr.push(Buffer.from(chunk));
        });

        child.on("error", reject);
        child.on("close", (exitCode) => {
            resolve({
                display: gradle.display,
                cwd,
                exitCode: exitCode ?? 1,
                stdout: Buffer.concat(stdout).toString("utf8"),
                stderr: Buffer.concat(stderr).toString("utf8"),
            });
        });
    });
}

function writeAttachment(resultsDir, name, content) {
    const source = `${randomUUID()}-attachment.txt`;
    writeFileSync(path.join(resultsDir, source), content, "utf8");
    return {
        name,
        source,
        type: "text/plain",
    };
}

function writeSandboxResult({ display, cwd, exitCode, stdout, stderr, start, stop }) {
    const resultsDir = path.join(sandboxDir, "build", "allure-results");
    mkdirSync(resultsDir, { recursive: true });

    const attachments = [];
    attachments.push(writeAttachment(resultsDir, "Sandbox build command", `${display}\n`));
    attachments.push(writeAttachment(resultsDir, "Sandbox build stdout", stdout || "(no stdout)\n"));
    if (stderr) {
        attachments.push(writeAttachment(resultsDir, "Sandbox build stderr", stderr));
    }

    const uuid = randomUUID();
    const result = {
        uuid,
        historyId: createHash("md5").update("sandbox.consumer.build").digest("hex"),
        name: "sandbox consumer build",
        fullName: "sandbox.consumer.build",
        status: exitCode === 0 ? "passed" : "failed",
        stage: "finished",
        description: "Runs the sample sandbox consumer build and records the command output as reviewable evidence.",
        steps: [
            {
                name: `Run ${display}`,
                status: exitCode === 0 ? "passed" : "failed",
                stage: "finished",
                steps: [],
                attachments,
                parameters: [
                    { name: "workingDir", value: path.relative(repoRoot, cwd) || "." },
                    { name: "exitCode", value: String(exitCode) },
                ],
                start,
                stop,
            },
        ],
        attachments: [],
        parameters: [
            { name: "command", value: display },
            { name: "workingDir", value: path.relative(repoRoot, cwd) || "." },
        ],
        labels: [
            { name: "host", value: os.hostname() },
            { name: "thread", value: `${process.pid}@${os.hostname()}` },
            { name: "framework", value: "custom" },
            { name: "language", value: "javascript" },
            { name: "package", value: "sandbox.consumer" },
            { name: "testClass", value: "SandboxBuild" },
            { name: "testMethod", value: "build" },
            { name: "suite", value: "sandbox" },
        ],
        start,
        stop,
    };

    if (exitCode !== 0) {
        result.statusDetails = {
            message: "Sandbox consumer build failed",
            trace: stderr || stdout || `${display} exited with code ${exitCode}`,
        };
    }

    writeFileSync(path.join(resultsDir, `${uuid}-result.json`), JSON.stringify(result), "utf8");
}

async function main() {
    if (process.env.ALLURE_SKIP_ROOT_BUILD !== "1") {
        const rootBuild = await runCommand(repoRoot);
        if (rootBuild.exitCode !== 0) {
            process.exit(rootBuild.exitCode);
        }
    }

    const start = Date.now();
    const sandboxBuild = await runCommand(sandboxDir);
    const stop = Date.now();
    writeSandboxResult({ ...sandboxBuild, start, stop });

    process.exit(sandboxBuild.exitCode);
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
