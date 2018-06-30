package application;

import java.io.File;
import java.io.IOException;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.CvType;

public class MyModel {
	public Mat m_matImage = null; // 存放采集阶段的归一化前的图像或者识别阶段的测试图像的矩阵
	public Mat m_matNormImage = null; // 图像采集阶段或者识别阶段归一化以后的人脸图像矩阵
	public Mat m_matTrainingImage = null; // 训练样本集矩阵M*N
	public Mat m_matEigenFace = null; // 特征脸矩阵(需存盘)
	public Mat m_matEigenTrainingSamples = null; // 训练样本集在特征脸空间的投影矩阵(需存盘)
	public Mat matMeanVector = null;
	public Mat eigen_train_sample = null;
	public Mat EigenFace = null;
	int numPerson; // 人的个数
	int numSamplePerPerson; // 每个人的训练样本数

	MyModel() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		numPerson = 8; // 40个人
		numSamplePerPerson = 5; // 每人5个样本
	}

	/** 读入训练样本集，保存在m_matTrainingImage矩阵中 */
	public void ReadTrainingSampleFiles(File file) { // file为训练样本集中的一个样本对象
		String fileParent = "file:///" + file.getParent();// 获得训练集所在目录
		String filePath = "file:///" + file.getPath();
		filePath = filePath.replace('\\', '/'); // 使用本地文件的URL路径
		fileParent = fileParent.replace('\\', '/'); // 使用本地文件的URL路径
		Image sample = new Image(filePath); // 读入一个训练样本
		// 创建训练样本矩阵：numPerson*numSamplePerPerson行，sample.getWidth()*sample.getHeight()列
		int img_w = (int) sample.getWidth(); // 训练样本宽度
		int img_h = (int) sample.getHeight(); // 训练样本高度
		m_matTrainingImage = new Mat(numPerson * numSamplePerPerson, img_w * img_h, CvType.CV_32F);
		for (int i = 1; i <= numPerson; i++) {// 第i个人
			for (int j = 1; j <= numSamplePerPerson; j++) // 第j个样本
			{
				float[] gray = new float[img_w * img_h]; // 存放1个人图像所有像素的灰度值
				String s = fileParent + "/s" + i + "_" + j + ".bmp"; // 图像si_j.bmp路径
				Image si_j = new Image(s);// 获取图像si_j.bmp
				PixelReader si_jPixelReader = si_j.getPixelReader(); // 图像si_j.bmp像素读入器
				int n = 0;
				for (int x = 0; x < img_w; x++) { // 按列遍历图像
					for (int y = 0; y < img_h; y++) {
						Color color = si_jPixelReader.getColor(x, y);
						gray[n] = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
						n++;
					}
				}
				m_matTrainingImage.put(((i - 1) * numSamplePerPerson + j - 1), 0, gray);
			}
		}
	}

	/** 该函数将m_matImage根据双眼坐标进行几何和灰度归一化，结果保存在m_matNormImage中 */
	public void NormalizeImage(int lefteye_x, int lefteye_y, int righteye_x, int righteye_y) {
		if (m_matImage.width() / 300 > m_matImage.height() / 300) {
			lefteye_x = lefteye_x * m_matImage.width() / 300;
			lefteye_y = lefteye_y * m_matImage.width() / 300;
			righteye_x = righteye_x * m_matImage.width() / 300;
			righteye_y = righteye_y * m_matImage.width() / 300;
		} else {
			lefteye_x = lefteye_x * m_matImage.height() / 300;
			lefteye_y = lefteye_y * m_matImage.height() / 300;
			righteye_x = righteye_x * m_matImage.height() / 300;
			righteye_y = righteye_y * m_matImage.height() / 300;
		}
		int centerX = (lefteye_x - righteye_x + 1) / 2 + righteye_x;
		int centerY = (lefteye_y - righteye_y + 1) / 2 + righteye_y;
		int x_center = m_matImage.width() / 2;
		int y_center = m_matImage.height() / 2;
		Point centerPoint = new Point(centerX, centerY);

		Mat matImage = new Mat((int) (m_matImage.height()), (int) (m_matImage.width()), CvType.CV_32F);
		double e = Math.atan(((float) (lefteye_y - righteye_y) / (lefteye_x - righteye_x)));
		matImage = Imgproc.getRotationMatrix2D(centerPoint, e * (180 / Math.PI), 1.0);
		System.out.println(e);

		centerX = (int) ((centerX - x_center) * Math.cos(e) - (centerY - y_center) * Math.sin(e) + x_center);
		centerY = (int) ((centerX - x_center) * Math.sin(e) + (centerY - y_center) * Math.cos(e) + y_center);
		m_matNormImage = m_matImage.clone();
		Imgproc.warpAffine(m_matImage, m_matNormImage, matImage, m_matNormImage.size(), Imgproc.INTER_NEAREST);

		CutImage(lefteye_x, lefteye_y, righteye_x, righteye_y);
	}

	public void CutImage(int lefteye_x, int lefteye_y, int righteye_x, int righteye_y) {
		int x_center = m_matImage.width() / 2;
		int y_center = m_matImage.height() / 2;

		int centerX = (lefteye_x - righteye_x + 1) / 2 + righteye_x;
		int centerY = (lefteye_y - righteye_y + 1) / 2 + righteye_y;
		System.out.println(centerX + " " + centerY + " " + x_center + " " + y_center);
		double distance1 = Math.pow(Math.pow(lefteye_x - righteye_x, 2) + Math.pow(lefteye_y - righteye_y, 2), 0.5);
		double distance2 = Math.pow(Math.pow(x_center - centerX, 2) + Math.pow(y_center - centerY, 2), 0.5);
		int x = 0, y = 0;
		x = (int) (centerX - distance1);
		y = (int) (centerY - (distance1) / 2);

		System.out.println(x + " " + y + " " + distance1 + " " + distance2);
		Rect rect = new Rect(x, y, (int) distance1 * 2, (int) distance1 * 2);

		m_matNormImage = new Mat(m_matNormImage, rect);
		Mat matImage = new Mat(48, 48, CvType.CV_32F);
		Imgproc.resize(m_matNormImage, m_matNormImage, matImage.size());
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_8UC1, 255, 0);
		Imgproc.equalizeHist(m_matNormImage, m_matNormImage);
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_32F, 1.0 / 255, 0);
	}

	/** 由训练集样本矩阵：m_matTrainingImage的得到特征脸矩阵，保存在数据域m_matEigenFace中 */
	public void CalculateEigenFaceMat() {
		int count = 0, countNum = 0;
		Mat normTrainFaceMat = new Mat(m_matTrainingImage.height(), m_matTrainingImage.width(), CvType.CV_32F);
		float[] trainingImgData = new float[m_matTrainingImage.height() * m_matTrainingImage.width()];
		m_matTrainingImage.get(0, 0, trainingImgData);
		// normTrainFaceMat(i,:)= trainFaceMat(i,:)-meanFaceMat;
		// 计算训练样本的平均值矩阵meanFaceMat

		matMeanVector = new Mat(m_matTrainingImage.height(), 1, CvType.CV_32F);// 按行求平均值得到的矩阵
		matMeanVector.create(1, m_matTrainingImage.cols(), CvType.CV_32F);
		for (int i = 0; i < m_matTrainingImage.cols(); i++) {
			Mat colRange = m_matTrainingImage.colRange(i, i + 1);
			Scalar mean = Core.mean(colRange);
			matMeanVector.put(0, i, mean.val[0]);
		}
		System.out.println("计算训练样本的平均值矩阵meanFaceMat:" + matMeanVector);
		System.out.println("计算训练样本的平均值矩阵meanFaceMat:" + matMeanVector.dump());

		// 计算规格化后的训练样本矩阵normTrainFaceMat，矩阵大小为M×N
		float[] matMeanVectorData = new float[m_matTrainingImage.height() * m_matTrainingImage.width()];
		matMeanVector.get(0, 0, matMeanVectorData);
		for (int i = 0; i < m_matTrainingImage.height(); i++) {

			for (int j = 0; j < m_matTrainingImage.width(); j++) {
				normTrainFaceMat.put(i, j, trainingImgData[i * m_matTrainingImage.width() + j] - matMeanVectorData[i]);
			}
			countNum++;
		}
		System.out.println("计算规格化后的训练样本矩阵normTrainFaceMat:" + normTrainFaceMat);
		// 计算normTrainFaceMat* normTrainFaceMat’矩阵的特征值和特征向量。
		Mat EigenValues = new Mat();
		Mat EigenVectors = new Mat();
		Mat EigenMat = new Mat();
		Core.mulTransposed(normTrainFaceMat, EigenMat, false);
		System.out.println("训练样本矩阵normTrainFaceMat及转置矩阵乘积:" + EigenMat);
		// Core.gemm(normTrainFaceMat, normTrainFaceMat.t(), 1.0, new Mat(), 0,
		// EigenMat);
		Core.eigen(EigenMat, EigenValues, EigenVectors);

		System.out.println("EigenValues:" + EigenValues.dump());
		System.out.println("EigenVectors:" + EigenVectors.dump());
		// 特征值默认按降序排序

		// 对特征空间进行降维

		float[] value = new float[EigenValues.height() * EigenValues.width()];
		EigenValues.get(0, 0, value);

		int m = 0;
		float ValuesSum = 0, sum = 0;
		for (int i = 0; i < EigenValues.rows(); i++) {
			ValuesSum = ValuesSum + value[i];
		}
		for (int i = 0; i < EigenValues.rows(); i++) {
			sum = sum + value[i];
			if (sum / ValuesSum >= 0.9) {
				m = i;
				break;
			}
		}
		m++;
		System.out.println("m=" + m);
		Mat eig_vec = new Mat();// 存放降维后的矩阵
		eig_vec.create(EigenVectors.rows(), m, CvType.CV_32F);
		for (int y = 0; y < m; y++) {
			for (int x = 0; x < EigenVectors.rows(); x++) {
				double a = EigenVectors.get(x, y)[0];
				double b = EigenValues.get(y, 0)[0];
				double c = a / Math.sqrt(b);
				eig_vec.put(x, y, c);

			}
		}
		// System.out.println("eig_vec:" + eig_vec.dump());
		// 训练样本的特征脸空间
		EigenFace = new Mat();
		Core.gemm(normTrainFaceMat.t(), eig_vec, 1.0, new Mat(), 0, EigenFace);
		// System.out.println("EigenFace:" + EigenFace);
		// System.out.println("EigenFace:" + EigenFace.dump());
		// 训练样本在特征脸空间的投影
		eigen_train_sample = new Mat();
		Core.gemm(normTrainFaceMat, EigenFace, 1.0, new Mat(), 0, eigen_train_sample);
		System.out.println("eigen_train_sample:" + eigen_train_sample);
		System.out.println("样本训练完成！！！");

	}

	/** 将数据域m_matEigenFace矩阵以二进制文件方式保存在当前文件夹中 */
	public void SaveEigenFaceMat() {
		// 使用java的二进制流进行文件保存
	}

	/** 从当前目录读入特征脸矩阵的二进制文件，将它存入到m_matEigenFace文件中 */
	public void ReadEigenFaceMatFromFile() throws IOException {

	}

	/** 将数据域m_matEigenTrainingSamples矩阵以二进制文件方式保存在当前文件夹中 */
	public void SaveEigenTrainingSamplesMat() {
		// 使用java的二进制流进行文件保存
	}

	/** 从当前目录读入训练样本集在特征脸空间的投影矩阵的二进制文件，将它存入到m_matEigenTrainingSamples文件中 */
	public void ReadEigenTrainingSamplesMatFromFile() throws IOException {

	}

	/** 计算最接近测试人脸的样本人脸:使用测试人脸的m_matNormImage矩阵与特征脸矩阵m_matEigenFace进行计算 */
	/** 该函数返回与测试人脸最接近的样本矩阵 */
	public int GetSimilarFaceMat() {
		System.out.println("GetSimilarFaceMat");
		// 将测试人脸展开为1×N矩阵testFaceMat N为一个图像所有像素按列相连的像素值

		float[] m_matNormImageData = new float[m_matNormImage.cols() * m_matNormImage.rows()];
		m_matNormImage.get(0, 0, m_matNormImageData);

		Mat testFaceMat = new Mat(1, m_matNormImage.width() * m_matNormImage.height(), CvType.CV_32F);
		for (int i = 0; i < m_matNormImage.height(); i++) {
			for (int j = 0; j < m_matNormImage.width(); j++) {
				testFaceMat.put(0, j * m_matNormImage.height() + i, m_matNormImageData[i * m_matNormImage.width() + j]);
			}
		}
		float[] testFaceMatData = new float[testFaceMat.width()];
		testFaceMat.get(0, 0, testFaceMatData);
		// testFaceMat.put(0, 0, );
		System.out.println("testFaceMat" + testFaceMat);
		// 求一张图片平均值矩阵meanFaceMat 前面已经求出
		float[] matMeanVectorData = new float[m_matNormImage.height() * m_matNormImage.width()];
		matMeanVector.get(0, 0, matMeanVectorData);

		System.out.println("matMeanVector" + matMeanVector);
		System.out.println("testFaceMat" + testFaceMat);

		// 计算规格化后的识别样本矩阵normTestFaceMat：
		Mat normTestFaceMat = new Mat(1, m_matNormImage.height() * m_matNormImage.width(), CvType.CV_32F);
		for (int i = 0; i < m_matNormImage.height() * m_matNormImage.width(); i++) {
			normTestFaceMat.put(0, i, testFaceMatData[i] - matMeanVectorData[i]);
		}
		System.out.println("normTestFaceMat" + normTestFaceMat);
		// 计算normTrainFaceMat* normTrainFaceMat’矩阵的特征值和特征向量。

		// 特征值默认按降序排序

		// 对特征空间进行降维

		// 训练样本的特征脸空间

		// 训练样本在特征脸空间的投影

		// EigenFace前面已经求出了
		Mat eigen_test_sample = new Mat();

		Core.gemm(normTestFaceMat, EigenFace, 1.0, new Mat(), 0, eigen_test_sample);

		System.out.println("eigen_train_sample:" + eigen_train_sample);
		System.out.println("eigen_test_sample:" + eigen_test_sample);
		System.out.println("eigen_train_sample:" + eigen_train_sample.dump());
		System.out.println("eigen_test_sample:" + eigen_test_sample.dump());

		float[] eigen_train_sampleData = new float[eigen_train_sample.height() * eigen_train_sample.width()];
		eigen_train_sample.get(0, 0, eigen_train_sampleData);
		double min_distance = 1000;
		int min_i = 0;
		for (int i = 0; i < eigen_train_sample.height(); i++) {
			Mat eigenTrainSample = new Mat(1, eigen_train_sample.width(), CvType.CV_32F);
			for (int j = 0; j < eigen_train_sample.width(); j++) {
				eigenTrainSample.put(0, j, eigen_train_sampleData[i * eigen_train_sample.width() + j]);
			}
			eigenTrainSample = eigen_train_sample.row(i).clone();
			double distance = Core.norm(eigen_test_sample, eigenTrainSample);
			if (min_distance > distance) {
				min_distance = distance;
				min_i = i;
			}
		}
		System.out.println("行数(图片位置):" + min_i + "\n");
		System.out.println("对应欧式距离:" + min_distance + "\n");
		if (min_distance > 30) {
			return -1;
		} else
			return min_i;
	}

	/** 将矩阵matImg中的值以图像的方式输出 */
	public WritableImage GetImageFromMatrix(Mat matImg) {
		if (matImg != null) {
			WritableImage wImage = new WritableImage(matImg.width(), matImg.height());
			// 得到像素写入器
			PixelWriter pixelWriter = wImage.getPixelWriter();
			if (matImg.channels() == 1) { // 单通道图像
				float[] gray = new float[matImg.height() * matImg.width()];
				matImg.get(0, 0, gray);
				// 遍历图像矩阵每个像素，将其写入到目标图像
				for (int y = 0; y < matImg.height(); y++) {
					for (int x = 0; x < matImg.width(); x++) {
						int pixelIndex = y * matImg.width() + x;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
			} else if (matImg.channels() == 3) { // 3通道图像
				// 遍历源图像每个像素，将其写入到目标图像
				for (int y = 0; y < matImg.height(); y++) {
					for (int x = 0; x < matImg.width(); x++) {
						int[] gray = new int[3];
						matImg.get(y, x, gray);
						Color color = Color.rgb(gray[2], gray[1], gray[0]);
						pixelWriter.setColor(x, y, color);
					}
				}
			}
			return wImage;
		}
		return null;
	}

	/** 将图像image存放在数据域m_matImage矩阵中 */
	public Mat GetGrayImgMatFromImage(Image image) {
		if (image != null) {
			PixelReader pixelReader = image.getPixelReader();
			m_matImage = new Mat((int) (image.getHeight()), (int) (image.getWidth()), CvType.CV_32F);
			// 遍历源图像每个像素，将其写入到目标图像矩阵
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					Color color = pixelReader.getColor(x, y);
					float gray = (float) ((color.getBlue() + color.getGreen() + color.getRed()) / 3.0);
					m_matImage.put(y, x, gray);
				}
			}
		}
		return m_matImage;
	}

	/** 从灰度图像集合(训练样本矩阵、特征脸矩阵)矩阵中抽取一行或一列组成二维图像 */
	public WritableImage GetImageFromGrayImagesMatrix(Mat matImages, // 图像集合矩阵
			int imgWidth, // 二维图像的宽度
			int index, // 取得行或者列的索引
			boolean byRow) // true为抽取一行，false为抽取一列
	{
		if (matImages != null || matImages.channels() != 1) {
			int widthMat = matImages.width();
			int heightMat = matImages.height();
			if (byRow == true) { // 按行抽取
				int imgHeight = widthMat / imgWidth; // 二维图像高度
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				// 得到像素写入器
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[widthMat];
				matImages.get(index, 0, gray);
				// 遍历图像每个像素
				for (int x = 0; x < wImage.getWidth(); x++) {
					for (int y = 0; y < wImage.getHeight(); y++) {
						int pixelIndex = x * imgHeight + y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
				return wImage;
			} else { // 按列抽取
				int imgHeight = heightMat / imgWidth; // 二维图像高度
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				// 得到像素写入器
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[heightMat];
				Mat matTImages = matImages.t(); // 矩阵转置
				matTImages.get(index, 0, gray);
				// 遍历图像每个像素
				for (int x = 0; x < wImage.getWidth(); x++) {
					for (int y = 0; y < wImage.getHeight(); y++) {
						int pixelIndex = x * imgHeight + y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
				return wImage;
			}
		}
		return null;
	}
}
