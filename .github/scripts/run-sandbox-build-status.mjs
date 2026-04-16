import { spawn } from "node:child_process";
import { randomUUID, createHash } from "node:crypto";
import { mkdirSync, writeFileSync } from "node:fs";
import os from "node:os";
import { cwd } from "node:process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "../..");
const sandboxDir = path.join(repoRoot, "sandbox");
const syntheticResultsDirArg = process.argv[2];

if (!syntheticResultsDirArg) {
    console.error("Usage: node .github/scripts/run-sandbox-build-status.mjs <results-dir>");
    process.exit(1);
}

const syntheticResultsDir = path.resolve(cwd(), syntheticResultsDirArg);

function sandboxBuildCommand() {
    if (process.platform === "win32") {
        return {
            command: "cmd.exe",
            args: ["/c", "sandbox\\gradlew.bat", "-p", "sandbox", "build", "-x", "test"],
            display: "sandbox\\gradlew.bat -p sandbox build -x test",
        };
    }

    return {
        command: "./sandbox/gradlew",
        args: ["-p", "sandbox", "build", "-x", "test"],
        display: "./sandbox/gradlew -p sandbox build -x test",
    };
}

function runSandboxBuild(gradle) {
    const stdout = [];
    const stderr = [];

    return new Promise((resolve, reject) => {
        const child = spawn(gradle.command, gradle.args, {
            cwd: repoRoot,
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
                command: gradle.display,
                exitCode: exitCode ?? 1,
                stdout: Buffer.concat(stdout).toString("utf8"),
                stderr: Buffer.concat(stderr).toString("utf8"),
            });
        });
    });
}

function writeAttachment(name, content) {
    const source = `${randomUUID()}-attachment.txt`;
    writeFileSync(path.join(syntheticResultsDir, source), content, "utf8");
    return {
        name,
        source,
        type: "text/plain",
    };
}

function writeSyntheticResult({ command, exitCode, stdout, stderr, start, stop }) {
    mkdirSync(syntheticResultsDir, { recursive: true });

    const attachments = [
        writeAttachment("Sandbox build command", `${command}\n`),
        writeAttachment("Sandbox build stdout", stdout || "(no stdout)\n"),
    ];

    if (stderr) {
        attachments.push(writeAttachment("Sandbox build stderr", stderr));
    }

    const matrixOs = process.env.MATRIX_OS || "unknown";
    const matrixJavaVersion = process.env.MATRIX_JAVA_VERSION || "unknown";
    const uuid = randomUUID();
    const result = {
        uuid,
        historyId: createHash("md5").update("sandbox.build.status").digest("hex"),
        name: "sandbox build status",
        fullName: "sandbox.build.status",
        status: exitCode === 0 ? "passed" : "failed",
        stage: "finished",
        description: "Reports the sandbox consumer build as a single CI status record with command output attached.",
        steps: [
            {
                name: "Run sandbox build",
                status: exitCode === 0 ? "passed" : "failed",
                stage: "finished",
                steps: [],
                attachments,
                parameters: [
                    { name: "os", value: matrixOs },
                    { name: "javaVersion", value: matrixJavaVersion },
                    { name: "exitCode", value: String(exitCode) },
                ],
                start,
                stop,
            },
        ],
        attachments: [],
        parameters: [
            { name: "command", value: command },
            { name: "os", value: matrixOs },
            { name: "javaVersion", value: matrixJavaVersion },
        ],
        labels: [
            { name: "host", value: os.hostname() },
            { name: "thread", value: `${process.pid}@${os.hostname()}` },
            { name: "framework", value: "custom" },
            { name: "language", value: "javascript" },
            { name: "package", value: "sandbox" },
            { name: "testClass", value: "SandboxBuild" },
            { name: "testMethod", value: "status" },
            { name: "suite", value: "sandbox" },
        ],
        start,
        stop,
    };

    if (exitCode !== 0) {
        result.statusDetails = {
            message: "Sandbox build failed",
            trace: stderr || stdout || `${command} exited with code ${exitCode}`,
        };
    }

    writeFileSync(path.join(syntheticResultsDir, `${uuid}-result.json`), JSON.stringify(result), "utf8");
}

async function main() {
    const start = Date.now();
    const gradle = sandboxBuildCommand();

    try {
        const build = await runSandboxBuild(gradle);
        const stop = Date.now();
        writeSyntheticResult({ ...build, start, stop });
        process.exit(build.exitCode);
    } catch (error) {
        const stop = Date.now();
        writeSyntheticResult({
            command: gradle.display,
            exitCode: 1,
            stdout: "",
            stderr: error instanceof Error ? error.stack || error.message : String(error),
            start,
            stop,
        });
        console.error(error);
        process.exit(1);
    }
}

main();
