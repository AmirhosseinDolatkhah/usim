package ahd.usim.engine.gui.swing;

import ahd.ulib.swingutils.ElementBasedPanel;
import ahd.ulib.utils.Utils;
import ahd.usim.engine.Constants;
import ahd.usim.engine.internal.Engine;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ahd.usim.engine.Constants.DEFAULT_GLFW_WINDOW_HEIGHT;
import static ahd.usim.engine.Constants.DEFAULT_GLFW_WINDOW_WIDTH;
import static ahd.usim.engine.gui.swing.ComponentBuilder.*;
import static ahd.usim.engine.gui.swing.ComponentBuilder.createTextEditor;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_NOTEQUAL;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE;
import static org.lwjgl.opengl.GL13C.GL_SAMPLE_ALPHA_TO_ONE;
import static org.lwjgl.opengl.GL13C.GL_SAMPLE_COVERAGE;
import static org.lwjgl.opengl.GL14C.GL_CONSTANT_ALPHA;
import static org.lwjgl.opengl.GL14C.GL_CONSTANT_COLOR;
import static org.lwjgl.opengl.GL14C.GL_ONE_MINUS_CONSTANT_ALPHA;
import static org.lwjgl.opengl.GL14C.GL_ONE_MINUS_CONSTANT_COLOR;
import static org.lwjgl.opengl.GL15C.GL_SRC1_ALPHA;
import static org.lwjgl.opengl.GL30C.GL_CLIP_DISTANCE0;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30C.GL_RASTERIZER_DISCARD;
import static org.lwjgl.opengl.GL31C.GL_PRIMITIVE_RESTART;
import static org.lwjgl.opengl.GL32C.GL_DEPTH_CLAMP;
import static org.lwjgl.opengl.GL32C.GL_PROGRAM_POINT_SIZE;
import static org.lwjgl.opengl.GL32C.GL_SAMPLE_MASK;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.opengl.GL33C.GL_ONE_MINUS_SRC1_ALPHA;
import static org.lwjgl.opengl.GL33C.GL_ONE_MINUS_SRC1_COLOR;
import static org.lwjgl.opengl.GL33C.GL_SRC1_COLOR;
import static org.lwjgl.opengl.GL40C.GL_SAMPLE_SHADING;
import static org.lwjgl.opengl.GL43C.*;

public class EngineRuntimeToolsPanel extends ElementBasedPanel {
    private final Engine engine;
    private final ScheduledThreadPoolExecutor updater;
    private ScheduledFuture<?> updateFuture;
    private int updateRate;
    private float updateLatency;
    private int updateCount;
    private final List<MultiGraphPanelForSampling> graphs;

    EngineRuntimeToolsPanel() {
        engine = Engine.getEngine();
        updater = new ScheduledThreadPoolExecutor(1);
        updateFuture = null;
        updateRate = 500;
        graphs = new ArrayList<>();
        updateLatency = Float.NaN;
        updateCount = 0;
        init();
    }

    @Override
    protected void init() {
        setSize(720, 620);
        add(new JPanel(new BorderLayout()) {{
            add(elementE("main-tabbedPane", new JTabbedPane() {{
                add("Stats", new JScrollPane(elementE("stats-panel", new JPanel() {{
                    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                    setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Real Time: "));
                        add(elementE("fps-label", new JLabel("FPS: " + Utils.round(engine.getFps(), 2)) {{
                            setForeground(Color.RED);
                        }}));
                        add(elementE("ups-label", new JLabel("| UPS: " + Utils.round(engine.getUps(), 2)) {{
                            setForeground(Color.GREEN);
                        }}));
                        add(elementE("ips-label", new JLabel("| IPS: " + Utils.round(engine.getIps(), 2)) {{
                            setForeground(Color.CYAN);
                        }}));
                        add(new JCheckBox("Graph") {{
                            setSelected(true);
                            addActionListener(e -> elementE("ps-graph").setVisible(isSelected()));
                        }});
                    }});
                    add(elementE("ps-graph", new MultiGraphPanelForSampling(350, 120) {{
                        addGraph("fps", 2, Color.RED, engine::getFps);
                        addGraph("ups", 2, Color.GREEN, engine::getUps);
                        addGraph("ips", 2, Color.CYAN, engine::getIps);
                    }}));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Accumulated: "));
                        add(elementE("fpsAccumulated-label", new JLabel("FPS: " + Utils.round(engine.getAccumulatedFps(), 2)) {{
                            setForeground(Color.RED);
                        }}));
                        add(elementE("upsAccumulated-label", new JLabel("| UPS: " + Utils.round(engine.getAccumulatedUps(), 2)) {{
                            setForeground(Color.GREEN);
                        }}));
                        add(elementE("ipsAccumulated-label", new JLabel("| IPS: " + Utils.round(engine.getAccumulatedIps(), 2)) {{
                            setForeground(Color.CYAN);
                        }}));
                        add(new JCheckBox("Graph") {{
                            setSelected(true);
                            addActionListener(e -> elementE("accumulatedPs-graph").setVisible(isSelected()));
                        }});
                    }});
                    add(elementE("accumulatedPs-graph", new MultiGraphPanelForSampling(350, 120) {{
                        addGraph("accumulatedFps", 2, Color.RED, engine::getAccumulatedFps);
                        addGraph("accumulatedUps", 2, Color.GREEN, engine::getAccumulatedUps);
                        addGraph("accumulatedIps", 2, Color.CYAN, engine::getAccumulatedIps);
                    }}));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Counts: "));
                        add(elementE("renderCount-label", new JLabel("Render: " + engine.getRenderCount()) {{
                            setForeground(Color.RED);
                        }}));
                        add(elementE("updateCount-label", new JLabel("| Update: " + engine.getUpdateCount()) {{
                            setForeground(Color.GREEN);
                        }}));
                        add(elementE("inputCount-label", new JLabel("| Input: " + engine.getInputCount()) {{
                            setForeground(Color.CYAN);
                        }}));
                        add(new JCheckBox("Graph") {{
                            setSelected(true);
                            addActionListener(e -> elementE("counts-graph").setVisible(isSelected()));
                        }});
                    }});
                    add(elementE("counts-graph", new MultiGraphPanelForSampling(350, 120) {{
                        addGraph("renderCount", 2, Color.RED, engine::getRenderCount);
                        addGraph("updateCount", 2, Color.GREEN, engine::getUpdateCount);
                        addGraph("inputCount", 2, Color.CYAN, engine::getInputCount);
                    }}));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Counts: "));
                        add(elementE("totalFrameLoss-label", new JLabel("Frame Loss: " + engine.getTotalFrameLoss()) {{
                            setForeground(Color.YELLOW);
                        }}));
                        add(elementE("pureTotalFrameLoss-label", new JLabel("| Pure Frame Loss: " + engine.getPureTotalFrameLoss()) {{
                            setForeground(Color.CYAN);
                        }}));
                        add(elementE("asyncUpdate-label", new JLabel("| Async Update: " + engine.getUpdateAsyncCount()) {{
                            setForeground(Color.ORANGE.darker());
                        }}));
                        add(new JCheckBox("Graph") {{
                            setSelected(true);
                            addActionListener(e -> elementE("async-counts-graph").setVisible(isSelected()));
                        }});
                    }});
                    add(elementE("async-counts-graph", new MultiGraphPanelForSampling(350, 120) {{
                        addGraph("totalFrameLossCount", 2, Color.YELLOW, engine::getTotalFrameLoss);
                        addGraph("totalPureFrameLossCount", 2, Color.CYAN, engine::getPureTotalFrameLoss);
                        addGraph("updateAsyncCount", 2, Color.ORANGE.darker(), engine::getUpdateAsyncCount);
                    }}));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Latencies: "));
                        add(elementE("renderTime-label", new JLabel("Render: " + Utils.round(engine.getRenderTime(), 2) + " ms") {{
                            setForeground(Color.RED);
                        }}));
                        add(elementE("updateTime-label", new JLabel("| Update: " + Utils.round(engine.getUpdateTime(), 2) + " ms") {{
                            setForeground(Color.GREEN);
                        }}));
                        add(elementE("inputTime-label", new JLabel("| Input: " + Utils.round(engine.getInputTime(), 2) + " ms") {{
                            setForeground(Color.CYAN);
                        }}));
                        add(new JCheckBox("Graph") {{
                            setSelected(true);
                            addActionListener(e -> elementE("latencies-graph").setVisible(isSelected()));
                        }});
                    }});
                    add(elementE("latencies-graph", new MultiGraphPanelForSampling(350, 120) {{
                        addGraph("renderTime", 2, Color.RED, engine::getRenderTime);
                        addGraph("updateTime", 2, Color.GREEN, engine::getUpdateTime);
                        addGraph("inputTime", 2, Color.CYAN, engine::getInputTime);
                    }}));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(elementE("frameLoss-label", new JLabel("Frame Loss: " + engine.getFrameLoss())));
                        add(elementE("cpuUsage-label", new JLabel("CPU Usage: " + Utils.round(Utils.cpuUsageByJVM() * 100, 2) + "%") {{
                            setForeground(Color.RED);
                        }}));
                        add(elementE("heapUsage-label",
                                new JLabel("Heap Usage: " + Utils.round(Utils.usedHeapSize() / (float) Constants.MEGA, 2))));
                        add(new JCheckBox("Graph") {{
                            setSelected(true);
                            addActionListener(e -> elementE("resourceUsage-graph").setVisible(isSelected()));
                        }});
                    }});
                    add(elementE("resourceUsage-graph", new MultiGraphPanelForSampling(350, 120) {{
                        addGraph("cpuUsage", 2, Color.RED, Utils::cpuUsageByJVM);
                    }}));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Sampling Rate (ms): "));
                        add(new JTextField(String.valueOf(updateRate)) {{
                            setPreferredSize(new Dimension(60, 30));
                            addActionListener(e -> {
                                try {
                                    setUpdateRate(Integer.parseInt(getText()));
                                } catch (NumberFormatException ignore) {
                                }
                                setText(String.valueOf(updateRate));
                            });
                        }});
                        add(elementE("updateLatency-label", new JLabel("Latency: " + updateLatency + "ms")));
                        add(elementE("sampleCount-label", new JLabel("| Sample Counts: " + updateCount)));
                        add(new JPanel(new GridLayout(0, 1)) {{
                            add(new JButton("Pause Monitoring") {{
                                setForeground(Color.GREEN);
                                setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                                addActionListener(e -> {
                                    var running = getForeground().equals(Color.GREEN);
                                    if (getForeground().equals(Color.GREEN))
                                        stopUpdater();
                                    else
                                        startUpdater();
                                    setForeground(running ? Color.RED : Color.GREEN);
                                    setText(running ? "Resume Monitoring" : "Pause Monitoring");
                                });
                            }});
                            add(new JButton("Clear Graphs") {{
                                addActionListener(
                                        e -> elementsE(MultiGraphPanelForSampling.class).forEach(MultiGraphPanelForSampling::reset));
                            }});
                        }});
                    }});
                    add(new JSeparator());
                    add(new JPanel(new GridLayout(0, 1)) {{
                        add(elementE("engineTimer-label", new JLabel("Engine Timer: " + engine.getTimer(), JLabel.CENTER) {{
                            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                        }}));
                        add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                            add(new JButton("Pause Engine") {{
                                setForeground(Color.GREEN);
                                addActionListener(e -> {
                                    var pause = getForeground().equals(Color.GREEN);
                                    setText(pause ? "Resume Engine" : "Pause Engine");
                                    setForeground(pause ? Color.RED : Color.GREEN);
                                    if (pause)
                                        engine.stop();
                                    else
                                        engine.start();
                                });
                            }});
                            add(new JButton("Pause Render") {{
                                setForeground(Color.GREEN);
                                addActionListener(e -> {
                                    var pause = getForeground().equals(Color.GREEN);
                                    setText(pause ? "Resume Render" : "Pause Render");
                                    setForeground(pause ? Color.RED : Color.GREEN);
                                    engine.setDoRender(!pause);
                                });
                            }});
                            add(new JButton("Pause Update") {{
                                setForeground(Color.GREEN);
                                addActionListener(e -> {
                                    var pause = getForeground().equals(Color.GREEN);
                                    setText(pause ? "Resume Update" : "Pause Update");
                                    setForeground(pause ? Color.RED : Color.GREEN);
                                    engine.setDoUpdate(!pause);
                                });
                            }});
                            add(new JButton("Pause Input") {{
                                setForeground(Color.GREEN);
                                addActionListener(e -> {
                                    var pause = getForeground().equals(Color.GREEN);
                                    setText(pause ? "Resume Input" : "Pause Input");
                                    setForeground(pause ? Color.RED : Color.GREEN);
                                    engine.setDoInput(!pause);
                                });
                            }});
                        }});
                    }});
                }})));
                add("Engine Settings", new JScrollPane(elementE("engineSettings-panel", new JPanel() {{
                    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                    setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JLabel("Targets: FPS: "));
                        add(elementE("fps-textField", new JTextField(String.valueOf(engine.getTargetFps())) {{
                            setPreferredSize(new Dimension(80, 30));
                            addActionListener(e -> {
                                try {
                                    engine.setTargetFps(Integer.parseInt(getText()));
                                } catch (NumberFormatException ignore) {}
                                setText(String.valueOf(engine.getTargetFps()));
                            });
                        }}));
                        add(new JLabel("UPS: "));
                        add(elementE("ups-textField", new JTextField(String.valueOf(engine.getTargetUps())) {{
                            setPreferredSize(new Dimension(80, 30));
                            addActionListener(e -> {
                                try {
                                    engine.setTargetUps(Integer.parseInt(getText()));
                                } catch (NumberFormatException ignore) {
                                }
                                setText(String.valueOf(engine.getTargetUps()));
                                textFieldE("fps-textField").setText(String.valueOf(engine.getTargetFps()));
                                textFieldE("ips-textField").setText(String.valueOf(engine.getTargetIps()));
                            });
                        }}));
                        add(new JLabel("IPS: "));
                        add(elementE("ips-textField", new JTextField(String.valueOf(engine.getTargetIps())) {{
                            setPreferredSize(new Dimension(80, 30));
                            addActionListener(e -> {
                                try {
                                    engine.setTargetIps(Integer.parseInt(getText()));
                                } catch (NumberFormatException ignore) {
                                }
                                setText(String.valueOf(engine.getTargetIps()));
                            });
                        }}));
                    }});
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glPolygonMode",
                            args -> engine.commitCommandsToMainThread(() -> glPolygonMode((int) args.get("face"), (int) args.get("mode"))),
                            () -> showErrorDialog("Bad Argument(s)"),
                            new ComponentBuilder.OptionBasedArg<>("face", Map.of("GL_FRONT", GL_FRONT, "GL_BACK", GL_BACK, "GL_FRONT_AND_BACK", GL_FRONT_AND_BACK)),
                            new ComponentBuilder.OptionBasedArg<>("mode", Map.of("GL_POINT", GL_POINT, "GL_LINE", GL_LINE, "GL_FILL", GL_FILL))));
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glEnable & glDisable", args -> engine.commitCommandsToMainThread(() -> {
                                if ((boolean) args.get("flag"))
                                    glEnable((int) args.get("state"));
                                else
                                    glDisable((int) args.get("state"));
                            }), () -> showErrorDialog("Bad Argument(s)"),
                            new ComponentBuilder.OptionBasedArg<>("state", new HashMap<>() {{
                                put("GL_BLEND", GL_BLEND);
                                put("GL_CLIP_DISTANCE0", GL_CLIP_DISTANCE0);
                                put("GL_COLOR_LOGIC_OP", GL_COLOR_LOGIC_OP);
                                put("GL_CULL_FACE", GL_CULL_FACE);
                                put("GL_DEBUG_OUTPUT", GL_DEBUG_OUTPUT);
                                put("GL_DEBUG_OUTPUT_SYNCHRONOUS", GL_DEBUG_OUTPUT_SYNCHRONOUS);
                                put("GL_DEPTH_CLAMP", GL_DEPTH_CLAMP);
                                put("GL_DEPTH_TEST", GL_DEPTH_TEST);
                                put("GL_DITHER", GL_DITHER);
                                put("GL_FRAMEBUFFER_SRGB", GL_FRAMEBUFFER_SRGB);
                                put("GL_LINE_SMOOTH", GL_LINE_SMOOTH);
                                put("GL_MULTISAMPLE", GL_MULTISAMPLE);
                                put("GL_POLYGON_OFFSET_FILL", GL_POLYGON_OFFSET_FILL);
                                put("GL_POLYGON_OFFSET_LINE", GL_POLYGON_OFFSET_LINE);
                                put("GL_POLYGON_OFFSET_POINT", GL_POLYGON_OFFSET_POINT);
                                put("GL_POLYGON_SMOOTH", GL_POLYGON_SMOOTH);
                                put("GL_PRIMITIVE_RESTART", GL_PRIMITIVE_RESTART);
                                put("GL_PRIMITIVE_RESTART_FIXED_INDEX", GL_PRIMITIVE_RESTART_FIXED_INDEX);
                                put("GL_RASTERIZER_DISCARD", GL_RASTERIZER_DISCARD);
                                put("GL_SAMPLE_ALPHA_TO_COVERAGE", GL_SAMPLE_ALPHA_TO_COVERAGE);
                                put("GL_SAMPLE_ALPHA_TO_ONE", GL_SAMPLE_ALPHA_TO_ONE);
                                put("GL_SAMPLE_COVERAGE", GL_SAMPLE_COVERAGE);
                                put("GL_SAMPLE_SHADING", GL_SAMPLE_SHADING);
                                put("GL_SAMPLE_MASK", GL_SAMPLE_MASK);
                                put("GL_SCISSOR_TEST", GL_SCISSOR_TEST);
                                put("GL_STENCIL_TEST", GL_STENCIL_TEST);
                                put("GL_TEXTURE_CUBE_MAP_SEAMLESS", GL_TEXTURE_CUBE_MAP_SEAMLESS);
                                put("GL_LINE_WIDTH", GL_LINE_WIDTH);
                                put("GL_PROGRAM_POINT_SIZE", GL_PROGRAM_POINT_SIZE);
                            }}),
                            new ComponentBuilder.OptionBasedArg<>("flag", Map.of("Enable", true, "Disable", false))));
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glLineWidth",
                            args -> engine.commitCommandsToMainThread(() -> glLineWidth(((Double) args.get("width")).floatValue())),
                            () -> showErrorDialog("Bad Arg(s)"),
                            new ComponentBuilder.NumberBasedArg<>("width", 0, Float.MAX_VALUE)));
                    add(new JSeparator());
                    // noinspection DuplicatedCode,SpellCheckingInspection
                    add(createFunctionCallerPanel("glBlendFunc",
                            args -> engine.commitCommandsToMainThread(() ->
                                    glBlendFunc((Integer) args.get("sfactor"), (Integer) args.get("dfactor"))),
                            () -> showErrorDialog("Bad Argument"),
                            new ComponentBuilder.OptionBasedArg<>("sfactor", new HashMap<>() {{
                                put("GL_ZERO", GL_ZERO);
                                put("GL_ONE", GL_ONE);
                                put("GL_SRC_COLOR", GL_SRC_COLOR);
                                put("GL_ONE_MINUS_SRC_COLOR", GL_ONE_MINUS_SRC_COLOR);
                                put("GL_DST_COLOR", GL_DST_COLOR);
                                put("GL_ONE_MINUS_DST_COLOR", GL_ONE_MINUS_DST_COLOR);
                                put("GL_SRC_ALPHA", GL_SRC_ALPHA);
                                put("GL_ONE_MINUS_SRC_ALPHA", GL_ONE_MINUS_SRC_ALPHA);
                                put("GL_DST_ALPHA", GL_DST_ALPHA);
                                put("GL_ONE_MINUS_DST_ALPHA", GL_ONE_MINUS_DST_ALPHA);
                                put("GL_CONSTANT_COLOR", GL_CONSTANT_COLOR);
                                put("GL_ONE_MINUS_CONSTANT_COLOR", GL_ONE_MINUS_CONSTANT_COLOR);
                                put("GL_CONSTANT_ALPHA", GL_CONSTANT_ALPHA);
                                put("GL_ONE_MINUS_CONSTANT_ALPHA", GL_ONE_MINUS_CONSTANT_ALPHA);
                                put("GL_SRC_ALPHA_SATURATE", GL_SRC_ALPHA_SATURATE);
                                put("GL_SRC1_COLOR", GL_SRC1_COLOR);
                                put("GL_ONE_MINUS_SRC1_COLOR", GL_ONE_MINUS_SRC1_COLOR);
                                put("GL_SRC1_ALPHA", GL_SRC1_ALPHA);
                                put("GL_ONE_MINUS_SRC1_ALPHA", GL_ONE_MINUS_SRC1_ALPHA);
                            }}),
                            new ComponentBuilder.OptionBasedArg<>("dfactor", new HashMap<>() {{
                                put("GL_ZERO", GL_ZERO);
                                put("GL_ONE", GL_ONE);
                                put("GL_SRC_COLOR", GL_SRC_COLOR);
                                put("GL_ONE_MINUS_SRC_COLOR", GL_ONE_MINUS_SRC_COLOR);
                                put("GL_DST_COLOR", GL_DST_COLOR);
                                put("GL_ONE_MINUS_DST_COLOR", GL_ONE_MINUS_DST_COLOR);
                                put("GL_SRC_ALPHA", GL_SRC_ALPHA);
                                put("GL_ONE_MINUS_SRC_ALPHA", GL_ONE_MINUS_SRC_ALPHA);
                                put("GL_DST_ALPHA", GL_DST_ALPHA);
                                put("GL_ONE_MINUS_DST_ALPHA", GL_ONE_MINUS_DST_ALPHA);
                                put("GL_CONSTANT_COLOR", GL_CONSTANT_COLOR);
                                put("GL_ONE_MINUS_CONSTANT_COLOR", GL_ONE_MINUS_CONSTANT_COLOR);
                                put("GL_CONSTANT_ALPHA", GL_CONSTANT_ALPHA);
                                put("GL_ONE_MINUS_CONSTANT_ALPHA", GL_ONE_MINUS_CONSTANT_ALPHA);
                                put("GL_SRC_ALPHA_SATURATE", GL_SRC_ALPHA_SATURATE);
                                put("GL_SRC1_COLOR", GL_SRC1_COLOR);
                                put("GL_ONE_MINUS_SRC1_COLOR", GL_ONE_MINUS_SRC1_COLOR);
                                put("GL_SRC1_ALPHA", GL_SRC1_ALPHA);
                                put("GL_ONE_MINUS_SRC1_ALPHA", GL_ONE_MINUS_SRC1_ALPHA);
                            }})
                    ));
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glColorMask", args -> engine.commitCommandsToMainThread(
                                    () -> glColorMask((boolean) args.get("red"), (boolean) args.get("green"), (boolean) args.get("blue"),
                                            (boolean) args.get("alpha"))), () -> showErrorDialog("Bad Arg(s)"),
                            new ComponentBuilder.OptionBasedArg<>("red", Map.of("TRUE", true, "FALSE", false)),
                            new ComponentBuilder.OptionBasedArg<>("green", Map.of("TRUE", true, "FALSE", false)),
                            new ComponentBuilder.OptionBasedArg<>("blue", Map.of("TRUE", true, "FALSE", false)),
                            new ComponentBuilder.OptionBasedArg<>("alpha", Map.of("TRUE", true, "FALSE", false))));
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glDepthFunc",
                            args -> engine.commitCommandsToMainThread(() -> glDepthFunc((int) args.get("func"))),
                            () -> showErrorDialog("Bad Arg(s)"),
                            new ComponentBuilder.OptionBasedArg<>("func", Map.of(
                                    "GL_NEVER", GL_NEVER,
                                    "GL_ALWAYS", GL_ALWAYS,
                                    "GL_LESS", GL_LESS,
                                    "GL_LEQUAL", GL_LEQUAL,
                                    "GL_EQUAL", GL_EQUAL,
                                    "GL_GREATER", GL_GREATER,
                                    "GL_GEQUAL", GL_GEQUAL,
                                    "GL_NOTEQUAL", GL_NOTEQUAL
                            ))));
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glfwSwapIntervals",
                            args -> engine.commitCommandsToMainThread(() -> glfwSwapInterval(((Double) args.get("interval")).intValue())),
                            () -> showErrorDialog("Bad Argument"), new ComponentBuilder.NumberBasedArg<>("interval", 0, Integer.MAX_VALUE)));
                    add(new JSeparator());
//                    add(createFunctionCallerPanel("glfwSetWindowAttrib"))
//                    add(new JSeparator());
                    add(createFunctionCallerPanel("glfwWindowHint",
                            args -> engine.commitCommandsToMainThread(() -> {
                                glfwWindowHint((int) args.get("hint"), (int) args.get("value"));
                                engine.rebuildWindow();
                            }),
                            () -> showErrorDialog("Bad Argument(s)"),
                            new ComponentBuilder.OptionBasedArg<>("hint", new HashMap<>() {{
                                put("GLFW_RESIZABLE", GLFW_RESIZABLE);
                                put("GLFW_VISIBLE", GLFW_VISIBLE);
                                put("GLFW_DECORATED", GLFW_DECORATED);
                                put("GLFW_FOCUSED", GLFW_FOCUSED);
                                put("GLFW_AUTO_ICONIFY", GLFW_AUTO_ICONIFY);
                                put("GLFW_FLOATING", GLFW_FLOATING);
                                put("GLFW_MAXIMIZED", GLFW_MAXIMIZED);
                                put("GLFW_CENTER_CURSOR", GLFW_CENTER_CURSOR);
                                put("GLFW_TRANSPARENT_FRAMEBUFFER", GLFW_TRANSPARENT_FRAMEBUFFER);
                                put("GLFW_FOCUS_ON_SHOW", GLFW_FOCUS_ON_SHOW);
                                put("GLFW_SCALE_TO_MONITOR", GLFW_SCALE_TO_MONITOR);
                                put("GLFW_STEREO", GLFW_STEREO);
                                put("GLFW_SRGB_CAPABLE", GLFW_SRGB_CAPABLE);
                                put("GLFW_DOUBLEBUFFER", GLFW_DOUBLEBUFFER);
                                put("GLFW_CONTEXT_NO_ERROR", GLFW_CONTEXT_NO_ERROR);
                                put("GLFW_OPENGL_FORWARD_COMPAT", GLFW_OPENGL_FORWARD_COMPAT);
                                put("GLFW_OPENGL_DEBUG_CONTEXT", GLFW_OPENGL_DEBUG_CONTEXT);
                                put("GLFW_COCOA_RETINA_FRAMEBUFFER", GLFW_COCOA_RETINA_FRAMEBUFFER);
                                put("GLFW_COCOA_GRAPHICS_SWITCHING", GLFW_COCOA_GRAPHICS_SWITCHING);
                            }}),
                            new ComponentBuilder.OptionBasedArg<>("value", Map.of("TRUE", GLFW_TRUE, "FALSE", GLFW_FALSE))));
                    add(new JSeparator());
                    add(createFunctionCallerPanel("glfwWindowHint",
                            args -> engine.commitCommandsToMainThread(() -> {
                                glfwWindowHint((int) args.get("hint"), ((Double) args.get("value")).intValue());
                                engine.rebuildWindow();
                            }),
                            () -> showErrorDialog("Bad Argument(s)"),
                            new ComponentBuilder.OptionBasedArg<>("hint", new HashMap<>() {{
                                put("GLFW_RED_BITS", GLFW_RED_BITS);
                                put("GLFW_GREEN_BITS", GLFW_GREEN_BITS);
                                put("GLFW_BLUE_BITS", GLFW_BLUE_BITS);
                                put("GLFW_ALPHA_BITS", GLFW_ALPHA_BITS);
                                put("GLFW_DEPTH_BITS", GLFW_DEPTH_BITS);
                                put("GLFW_STENCIL_BITS", GLFW_STENCIL_BITS);
                                put("GLFW_ACCUM_RED_BITS", GLFW_ACCUM_RED_BITS);
                                put("GLFW_ACCUM_GREEN_BITS", GLFW_ACCUM_GREEN_BITS);
                                put("GLFW_ACCUM_BLUE_BITS", GLFW_ACCUM_BLUE_BITS);
                                put("GLFW_ACCUM_ALPHA_BITS", GLFW_ACCUM_ALPHA_BITS);
                                put("GLFW_AUX_BUFFERS", GLFW_AUX_BUFFERS);
                                put("GLFW_SAMPLES", GLFW_SAMPLES);
                                put("GLFW_REFRESH_RATE", GLFW_REFRESH_RATE);
                            }}),
                            new ComponentBuilder.NumberBasedArg<>("value", 0, Integer.MAX_VALUE)));
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JCheckBox("Use Render Synchronizer") {{
                            setSelected(engine.isUseRenderSynchronizer());
                            addActionListener(e -> engine.setUseRenderSynchronizer(isSelected()));
                        }});
                        add(new JCheckBox("Use Update Synchronizer") {{
                            setSelected(engine.isUseUpdateSynchronizer());
                            addActionListener(e -> engine.setUseUpdateSynchronizer(isSelected()));
                        }});
                    }});
                    add(new JSeparator());
                    add(new JPanel(new FlowLayout(FlowLayout.CENTER)) {{
                        add(new JButton("Turn off") {{addActionListener(e -> engine.turnoff());}});
                    }});
                    // temp
                    add(new JSeparator());
                    add(new JButton("Get Swing Dialog") {{
                        addActionListener(e -> new JDialog() {{
                            setSize(DEFAULT_GLFW_WINDOW_WIDTH, DEFAULT_GLFW_WINDOW_HEIGHT);
                            setLayout(new BorderLayout());
                            add(new GlfwPanel(this));
                            SwingUtilities.invokeLater(() -> this.setVisible(true));
                        }});
                    }});
                }})));
                add("Entity", new JScrollPane(createEntityPanel(EngineRuntimeToolsPanel.this)));
                add("Vertex Shader", createTextEditor(code -> {

                        }, () -> Utils.getFileAsStringElseEmpty(Constants.VERTEX_SHADER_FILE_RESOURCE_PATH),
                        code -> engine.commitCommandsToMainThread(() -> {}),
                        Utils.getFileAsStringElseEmpty(Constants.VERTEX_SHADER_FILE_RESOURCE_PATH)));
                add("Fragment Shader", createTextEditor(code -> {

                        }, () -> Utils.getFileAsStringElseEmpty(Constants.FRAGMENT_SHADER_FILE_RESOURCE_PATH),
                        code -> {},
                        Utils.getFileAsStringElseEmpty(Constants.FRAGMENT_SHADER_FILE_RESOURCE_PATH)));
                add("Camera", new JScrollPane(new JPanel() {{
                    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                    add(new JPanel(new GridLayout(0, 1)) {{
                        setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
                        add(elementE("cameraPosition-label", new JLabel("Camera Position: " +
                                engine.getCamera().getPosition().toString(DecimalFormat.getInstance()), JLabel.CENTER)));
                        add(elementE("cameraRotation-label", new JLabel(
                                "Camera Rotation: " + engine.getCamera().getRotation().toString(DecimalFormat.getInstance()), JLabel.CENTER)));
                    }});

                }}));
                add("Shader Manager", new JPanel() {{
                    setLayout(new MigLayout());
                    add(new JScrollPane((new JTree(new DefaultMutableTreeNode() {{
                        add(new DefaultMutableTreeNode("Vertex Shader"));
                        add(new DefaultMutableTreeNode("Fragment Shader"));
                        add(new DefaultMutableTreeNode("Geometry Shader"));
                    }}) {{
                        addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                var path = getPathForLocation(e.getX(), e.getY());
                                if (path == null)
                                    return;
                                System.out.println(path);
                            }
                        });
                        setCellRenderer(new DefaultTreeCellRenderer() {
                            {
                                setLeafIcon(new ImageIcon(Constants.DEFAULT_RESOURCE_ROOT_PATH + "icons/usim-icon.png"));
                            }
                        });
                    }})), "left");
                }});
            }}), BorderLayout.CENTER);
        }});
        graphs.addAll(elementsE(MultiGraphPanelForSampling.class));
        startUpdater();
    }

    void startUpdater() {
        if (updateFuture != null)
            return;
        updateFuture = updater.scheduleAtFixedRate(this::updateElementsE, 0, updateRate, TimeUnit.MILLISECONDS);
    }

    void stopUpdater() {
        if (updateFuture == null)
            return;
        updateFuture.cancel(true);
        updateFuture = null;
    }

    void setUpdateRate(int millis) {
        updateRate = millis;
        var cancelled = updateFuture.isCancelled();
        stopUpdater();
        if (!cancelled)
            startUpdater();
    }

    @Override
    public void updateElementsE() {
        var t = System.nanoTime();

        graphs.forEach(MultiGraphPanelForSampling::update);

        labelE("fps-label").setText("FPS: " + Utils.round(engine.getFps(), 2));
        labelE("ups-label").setText("| UPS: " + Utils.round(engine.getUps(), 2));
        labelE("ips-label").setText("| IPS: " + Utils.round(engine.getIps(), 2));

        labelE("fpsAccumulated-label").setText("FPS: " + Utils.round(engine.getAccumulatedFps(), 2));
        labelE("upsAccumulated-label").setText("| UPS: " + Utils.round(engine.getAccumulatedUps(), 2));
        labelE("ipsAccumulated-label").setText("| IPS: " + Utils.round(engine.getAccumulatedIps(), 2));

        labelE("renderTime-label").setText("Render: " + Utils.round(engine.getRenderTime(), 2) + " ms");
        labelE("updateTime-label").setText("| Update: " + Utils.round(engine.getUpdateTime(), 2) + " ms");
        labelE("inputTime-label").setText("| Input: " + Utils.round(engine.getInputTime(), 2) + " ms");

        labelE("frameLoss-label").setText("Frame Loss: " + engine.getFrameLoss());
        labelE("cpuUsage-label").setText("| CPU Usage: " + Utils.round(Utils.cpuUsageByJVM() * 100, 2) + "%");
        labelE("heapUsage-label").setText("| Heap Usage: " + Utils.round(Utils.usedHeapSize() / (float) Constants.MEGA, 2) + " MB");

        labelE("renderCount-label").setText("Render: " + engine.getRenderCount());
        labelE("updateCount-label").setText("| Update: " + engine.getUpdateCount());
        labelE("inputCount-label").setText("| Input: " + engine.getInputCount());
        labelE("totalFrameLoss-label").setText("| Frame Loss: " + engine.getTotalFrameLoss());
        labelE("pureTotalFrameLoss-label").setText("| Pure Frame Loss: " + engine.getPureTotalFrameLoss());
        labelE("asyncUpdate-label").setText("| Async Update: " + engine.getUpdateAsyncCount());

        labelE("cameraPosition-label").setText("Camera Position: " + engine.getCamera().getPosition().toString(DecimalFormat.getInstance()));
        labelE("cameraRotation-label").setText("Camera Rotation: " + engine.getCamera().getRotation().toString(DecimalFormat.getInstance()));

        labelE("updateLatency-label").setText("Latency: " + Utils.round(updateLatency, 2) + " ms");
        labelE("sampleCount-label").setText("| Sample Counts: " + ++updateCount);

        labelE("engineTimer-label").setText("Engine Timer: " + engine.getTimer());

        updateLatency = (System.nanoTime() - t) / Constants.MILLION_F;
    }
}
